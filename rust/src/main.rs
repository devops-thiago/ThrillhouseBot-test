use std::collections::HashMap;
use std::io::{BufRead, BufReader, Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, Mutex};
use std::thread;

use turbine_alerts::alerts::{valid_turbine_id, Alert, AlertGate, Notifier};
use turbine_alerts::calibration::CalibrationTable;
use turbine_alerts::config::Config;
use turbine_alerts::ingest::{dedup, evaluate, parse_batch, Evaluation};
use turbine_alerts::registry::{load_fleet, HttpRegistry, Turbine};

const LISTEN_ADDR: &str = "0.0.0.0:8085";
const CALIBRATION_PATH: &str = "/etc/turbine-alerts/calibration.csv";
const RMS_WINDOW: usize = 4096;
const MAX_BODY_BYTES: usize = 4 << 20;
/// Zone C (unsatisfactory) and above page on-call.
const PAGE_ZONE: usize = 2;

struct App {
    cfg: Config,
    fleet: HashMap<String, Turbine>,
    calibration: CalibrationTable,
    gate: AlertGate,
    notifier: Notifier,
    latest: Mutex<HashMap<String, Evaluation>>,
}

impl App {
    fn site_of(&self, turbine_id: &str) -> String {
        self.fleet.get(turbine_id).map_or_else(|| "unregistered".to_string(), |t| t.site.clone())
    }
}

fn main() -> Result<(), String> {
    let cfg = Config::from_env()?;
    let registry = HttpRegistry::new(&cfg.registry_addr, &cfg.registry_token);
    let fleet = load_fleet(&registry)?.into_iter().map(|t| (t.id.clone(), t)).collect();
    let calibration_text = std::fs::read_to_string(CALIBRATION_PATH).map_err(|e| e.to_string())?;
    let calibration = CalibrationTable::parse(&calibration_text)?;
    let notifier = Notifier { curl_path: "/usr/local/bin/curl".to_string() };
    let app = Arc::new(App { cfg, fleet, calibration, gate: AlertGate::default(), notifier, latest: Mutex::default() });

    let sweeper = Arc::clone(&app);
    thread::spawn(move || loop {
        thread::sleep(sweeper.cfg.sweep_interval);
        sweep(&sweeper);
    });

    let listener = TcpListener::bind(LISTEN_ADDR).map_err(|e| e.to_string())?;
    for stream in listener.incoming().flatten() {
        let app = Arc::clone(&app);
        thread::spawn(move || handle(&app, stream).unwrap_or_else(|e| eprintln!("request failed: {e}")));
    }
    Ok(())
}

/// Re-pages every turbine whose latest evaluation is still in the worst zone.
fn sweep(app: &App) {
    let worst = app.cfg.thresholds_mm_s.len();
    let mut critical_turbines: Vec<Alert> = Vec::new();
    for ev in app.latest.lock().unwrap().values() {
        let site = app.site_of(&ev.turbine_id);
        critical_turbines.push(Alert { turbine_id: ev.turbine_id.clone(), site, rms: ev.rms, zone: ev.zone });
    }
    if critical_turbines.is_empty() {
        return;
    }
    eprintln!("sweep: {} turbines in zone {worst}", critical_turbines.len());
    for alert in &critical_turbines {
        if let Err(e) = app.gate.fire(alert, &app.notifier) {
            eprintln!("sweep page for {} failed: {e}", alert.turbine_id);
        }
    }
}

fn handle(app: &App, mut stream: TcpStream) -> Result<(), String> {
    let mut reader = BufReader::new(stream.try_clone().map_err(|e| e.to_string())?);
    let mut request_line = String::new();
    reader.read_line(&mut request_line).map_err(|e| e.to_string())?;
    let mut content_length = 0usize;
    let mut header = String::new();
    while reader.read_line(&mut header).map_err(|e| e.to_string())? > 0 && !header.trim().is_empty() {
        if let Some((name, value)) = header.split_once(':').filter(|(n, _)| n.eq_ignore_ascii_case("content-length")) {
            content_length = value.trim().parse().ok().filter(|&n| n <= MAX_BODY_BYTES).ok_or(format!("bad {name}"))?;
        }
        header.clear();
    }
    let mut body = vec![0u8; content_length];
    reader.read_exact(&mut body).map_err(|e| e.to_string())?;

    let (status, reply) = match request_line.split_whitespace().collect::<Vec<_>>()[..] {
        ["POST", path, _] => match path.strip_prefix("/readings/") {
            Some(id) if valid_turbine_id(id) => ingest(app, id, &String::from_utf8_lossy(&body)),
            _ => ("400 Bad Request", "invalid turbine id".to_string()),
        },
        ["GET", "/healthz", _] => ("200 OK", "ok".to_string()),
        _ => ("404 Not Found", "not found".to_string()),
    };
    write!(stream, "HTTP/1.1 {status}\r\nContent-Length: {}\r\n\r\n{reply}", reply.len())
        .map_err(|e| e.to_string())
}

fn ingest(app: &App, turbine_id: &str, body: &str) -> (&'static str, String) {
    let readings = match parse_batch(body) { Ok(r) => dedup(r), Err(e) => return ("400 Bad Request", e) };
    let ev = evaluate(turbine_id, &readings, &app.calibration, &app.cfg.thresholds_mm_s, RMS_WINDOW);
    let Ok(ev) = ev.map_err(|e| eprintln!("{turbine_id}: {e}")) else {
        return ("422 Unprocessable Entity", "readings could not be calibrated".to_string());
    };
    app.latest.lock().unwrap().insert(turbine_id.to_string(), ev.clone());
    if ev.zone >= PAGE_ZONE {
        let alert = Alert { turbine_id: ev.turbine_id.clone(), site: app.site_of(turbine_id), rms: ev.rms, zone: ev.zone };
        if let Err(e) = app.gate.fire(&alert, &app.notifier) {
            return ("502 Bad Gateway", e);
        }
    }
    ("202 Accepted", format!("rms={:.2} zone={}", ev.rms, ev.zone))
}
