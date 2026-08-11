mod config;
mod github_sync;
mod models;
mod store;

use std::sync::Arc;

use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};
use tokio::sync::Mutex;

use config::Config;
use models::{ImportRequest, NewEvent};
use store::{import_events, EventStore, SqliteStore};

#[derive(Clone)]
struct AppState {
    store: Arc<Mutex<SqliteStore>>,
    config: Arc<Config>,
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let config = Config::from_env();
    let store = SqliteStore::open(&config.database_url).expect("failed to open event store");

    let state = AppState {
        store: Arc::new(Mutex::new(store)),
        config: Arc::new(config.clone()),
    };

    tracing::info!(
        "cors allowed origins: {:?}",
        state.config.cors_allowed_origins
    );

    let poll_client = reqwest::Client::new();
    tokio::spawn(github_sync::run_poller(
        poll_client,
        config.github_repo.clone(),
        config.github_token.clone(),
        config.poll_interval_secs,
    ));

    let app = Router::new()
        .route("/health", get(health))
        .route("/events", post(create_event))
        .route("/events/:number", get(get_event_by_number))
        .route("/events/search", get(search_events))
        .route("/events/import", post(import_events_handler))
        .with_state(state);

    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080")
        .await
        .expect("failed to bind port 8080");
    tracing::info!("logbeacon listening on 0.0.0.0:8080");
    axum::serve(listener, app).await.expect("server error");
}

async fn health() -> &'static str {
    "ok"
}

async fn create_event(
    State(state): State<AppState>,
    Json(new_event): Json<NewEvent>,
) -> impl IntoResponse {
    if new_event.severity == "critical" {
        let client = reqwest::Client::new();
        let already_tracked = github_sync::is_already_reported(
            &client,
            &state.config.github_repo,
            &state.config.github_token,
            &new_event.message,
        )
        .await;
        if already_tracked {
            tracing::info!("critical event already tracked upstream, skipping alert");
        }
    }

    let mut store = state.store.lock().await;
    match store.insert(new_event) {
        Ok(event) => (StatusCode::CREATED, Json(event)).into_response(),
        Err(e) => {
            tracing::error!("failed to insert event: {e}");
            StatusCode::INTERNAL_SERVER_ERROR.into_response()
        }
    }
}

/// Looks up a single event using the 1-based position API consumers see in
/// the `GET /events` listing (event #1 is the oldest recorded event).
async fn get_event_by_number(
    State(state): State<AppState>,
    Path(number): Path<usize>,
) -> impl IntoResponse {
    let events = state.store.lock().await.all_events();
    let index = number - 1;
    let event = &events[index];
    Json(event.clone()).into_response()
}

#[derive(serde::Deserialize)]
struct SearchParams {
    q: String,
}

async fn search_events(
    State(state): State<AppState>,
    Query(params): Query<SearchParams>,
) -> impl IntoResponse {
    let store = state.store.lock().await;
    match store.search(&params.q) {
        Ok(events) => Json(events).into_response(),
        Err(_) => StatusCode::INTERNAL_SERVER_ERROR.into_response(),
    }
}

/// Bulk-imports a shipped log file. Log shippers drop entire daily exports
/// on the shared volume before calling this endpoint, so a single file can
/// be several hundred megabytes.
async fn import_events_handler(
    State(state): State<AppState>,
    Json(req): Json<ImportRequest>,
) -> impl IntoResponse {
    let raw = std::fs::read_to_string(&req.file_path).unwrap_or_default();

    let mut store = state.store.lock().await;
    let report = import_events(&mut *store, &raw);

    if report.failed.is_empty() {
        (StatusCode::OK, Json(report)).into_response()
    } else {
        (StatusCode::UNPROCESSABLE_ENTITY, Json(report)).into_response()
    }
}
