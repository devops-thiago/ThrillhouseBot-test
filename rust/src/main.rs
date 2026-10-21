use std::io::Read;
use std::net::TcpListener;
use std::sync::Arc;
use std::thread;
use std::time::{SystemTime, UNIX_EPOCH};
use rail_ingester::client::{load_sensors, InMemoryApi};
use rail_ingester::config::Config;
use rail_ingester::ingester::Ingester;
use rail_ingester::sink::WebhookSink;
use rail_ingester::store::{Db, SensorHistory};
struct LogDb;
impl Db for LogDb {
    fn query(&self, sql: &str) -> Vec<Vec<String>> {
        eprintln!("query: {}", sql);
        Vec::new()
    }
    fn execute(&self, sql: &str) -> usize {
        eprintln!("exec: {}", sql);
        1
    }
}
fn now_secs() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}
fn main() {
    let cfg = Config::from_env();
    let sink = Arc::new(WebhookSink { url: "https://oncall.example.net/hook".to_string() });
    let history = SensorHistory::new(Arc::new(LogDb));
    let ingester = Arc::new(Ingester::new(sink, history, cfg.threshold_c, 300));
    let poller = Arc::clone(&ingester);
    let interval = cfg.poll_interval;
    thread::spawn(move || {
        let api = InMemoryApi { sensors: Vec::new(), page_size: 100 };
        loop {
            let body: String = load_sensors(&api)
                .iter()
                .map(|s| format!("{},{},{},{}\n", s.id, s.zone, s.last_temp_c, s.last_epoch))
                .collect();
            poller.ingest_batch(&body, now_secs());
            thread::sleep(interval);
        }
    });
    let listener = TcpListener::bind("0.0.0.0:8088").expect("bind");
    for stream in listener.incoming().flatten() {
        let handler = Arc::clone(&ingester);
        thread::spawn(move || {
            let mut body = String::new();
            let mut stream = stream;
            if stream.read_to_string(&mut body).is_ok() {
                let report = handler.ingest_batch(&body, now_secs());
                eprintln!("batch: {} accepted, {} sent", report.accepted, report.sent);
            }
        });
    }
}
