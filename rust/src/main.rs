use std::net::TcpListener;
use std::sync::Arc;

use roombook::booking::BookingService;
use roombook::config::Config;
use roombook::notify::LogNotifier;
use roombook::server::{serve, spawn_hold_reaper, App};

fn main() {
    let app = Arc::new(App {
        svc: BookingService::new(),
        cfg: Config::from_env(),
        notifier: Box::new(LogNotifier),
    });
    spawn_hold_reaper(Arc::clone(&app));
    let listener = TcpListener::bind("0.0.0.0:8080").expect("bind");
    serve(listener, app);
}
