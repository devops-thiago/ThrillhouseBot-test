use axum::extract::State;
use axum::http::StatusCode;
use axum::routing::{get, post};
use axum::{Json, Router};
use hookrelay::{db, delivery, health, signer, subscriber};
use rusqlite::Connection;
use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};

#[derive(Clone)]
struct AppState {
    db: Arc<Mutex<Connection>>,
    http: reqwest::Client,
    health: Arc<health::HealthState>,
    directory_url: String,
    allowed_topics: Vec<String>,
}

#[derive(Debug, Deserialize)]
struct EventIn { id: String, topic: String, payload: serde_json::Value }

#[derive(Debug, Serialize)]
struct DeliveryReport { delivered: usize, skipped: usize }

#[derive(Debug, Deserialize)]
struct SyncRequest { topic: String }

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let db_path = std::env::var("HOOKRELAY_DB_PATH").unwrap_or_else(|_| "hookrelay.db".to_string());
    let directory_url = std::env::var("HOOKRELAY_SUBSCRIBER_API_URL").unwrap_or_else(|_| "http://localhost:9000".to_string());
    let port = std::env::var("HOOKRELAY_PORT").unwrap_or_else(|_| "8080".to_string());
    let allowed_topics: Vec<String> = std::env::var("HOOKRELAY_ALLOWED_TOPICS").unwrap_or_default()
        .split(',').map(|s| s.trim().to_string()).filter(|s| !s.is_empty()).collect();

    let conn = Connection::open(&db_path).expect("failed to open database");
    db::init_db(&conn).expect("failed to initialize schema");

    let state = AppState {
        db: Arc::new(Mutex::new(conn)),
        http: reqwest::Client::new(),
        health: Arc::new(health::HealthState::default()),
        directory_url,
        allowed_topics,
    };

    let app = Router::new()
        .route("/events", post(ingest_event))
        .route("/events/batch", post(ingest_batch))
        .route("/subscribers/sync", post(sync_subscribers))
        .route("/health", get(health_check))
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{}", port)).await.unwrap();
    tracing::info!("hookrelay listening on 0.0.0.0:{}", port);
    axum::serve(listener, app).await.unwrap();
}

/// Delivers `payload` to `sub`, then records the outcome for health
/// reporting and the delivery log. Returns whether delivery succeeded.
async fn deliver_and_record(state: &AppState, event_id: &str, sub: &db::Subscriber, payload: &str) -> bool {
    let outcome = delivery::deliver_to_subscriber(&state.http, &signer::HmacSigner, sub, payload).await;
    state.health.record_attempt(event_id, sub.id, outcome.success);
    let conn = state.db.lock().unwrap();
    let _ = db::record_delivery(&conn, event_id, sub.id, outcome.success);
    outcome.success
}

async fn ingest_event(State(state): State<AppState>, Json(event): Json<EventIn>) -> (StatusCode, Json<DeliveryReport>) {
    if !state.allowed_topics.is_empty() && !state.allowed_topics.contains(&event.topic) {
        return (StatusCode::FORBIDDEN, Json(DeliveryReport { delivered: 0, skipped: 0 }));
    }

    let subscribers = {
        let conn = state.db.lock().unwrap();
        db::find_subscribers_by_topic(&conn, &event.topic).unwrap_or_default()
    };

    let payload = event.payload.to_string();
    let mut delivered = 0;
    let mut skipped = 0;
    for sub in &subscribers {
        if deliver_and_record(&state, &event.id, sub, &payload).await {
            delivered += 1;
        } else {
            skipped += 1;
        }
    }

    (StatusCode::OK, Json(DeliveryReport { delivered, skipped }))
}

/// Ingests a batch of events, skipping any already delivered to a given
/// subscriber. Batches can carry the full backlog after an outage, so this
/// loads the delivery log once up front rather than querying per event.
async fn ingest_batch(State(state): State<AppState>, Json(events): Json<Vec<EventIn>>) -> Json<DeliveryReport> {
    let log = {
        let conn = state.db.lock().unwrap();
        db::load_delivery_log(&conn).unwrap_or_default()
    };

    let mut delivered = 0;
    let mut skipped = 0;
    for event in &events {
        let subscribers = {
            let conn = state.db.lock().unwrap();
            db::find_subscribers_by_topic(&conn, &event.topic).unwrap_or_default()
        };

        for sub in &subscribers {
            if delivery::is_already_delivered(&event.id, sub.id, &log) {
                skipped += 1;
                continue;
            }
            let payload = event.payload.to_string();
            if deliver_and_record(&state, &event.id, sub, &payload).await {
                delivered += 1;
            } else {
                skipped += 1;
            }
        }
    }

    Json(DeliveryReport { delivered, skipped })
}

async fn sync_subscribers(State(state): State<AppState>, Json(req): Json<SyncRequest>) -> Json<serde_json::Value> {
    match subscriber::fetch_subscribers_for_topic(&state.http, &state.directory_url, &req.topic).await {
        Ok(records) => {
            let conn = state.db.lock().unwrap();
            for r in &records {
                let _ = db::upsert_subscriber(&conn, r.id, &r.name, &r.topic, &r.webhook_url, &r.secret);
            }
            Json(serde_json::json!({ "synced": records.len() }))
        }
        Err(e) => {
            tracing::error!("failed to sync subscribers for {}: {}", req.topic, e);
            Json(serde_json::json!({ "synced": 0, "error": e.to_string() }))
        }
    }
}

async fn health_check(State(state): State<AppState>) -> (StatusCode, Json<serde_json::Value>) {
    if state.health.is_degraded() {
        (StatusCode::SERVICE_UNAVAILABLE, Json(serde_json::json!({ "status": "degraded" })))
    } else {
        (StatusCode::OK, Json(serde_json::json!({ "status": "ok" })))
    }
}
