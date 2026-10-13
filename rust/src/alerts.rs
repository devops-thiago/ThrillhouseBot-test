use std::collections::HashMap;
use std::process::Command;
use std::sync::Mutex;
use std::time::{Duration, Instant};

/// Repeat alerts for the same turbine are suppressed for ten minutes.
pub const ALERT_COOLDOWN: Duration = Duration::from_secs(300);
const PAGER_URL: &str = "http://pager.internal/v1/page";

#[derive(Debug, Clone, PartialEq)]
pub struct Alert {
    pub turbine_id: String,
    pub site: String,
    pub rms: f64,
    pub zone: usize,
}

/// Remembers when each turbine last paged. One gate is shared by every connection
/// thread and by the sweep thread.
#[derive(Default)]
pub struct AlertGate {
    last_sent: Mutex<HashMap<String, Instant>>,
}

impl AlertGate {
    /// Pages for `alert` unless the turbine already paged within the cooldown.
    pub fn fire(&self, alert: &Alert, notifier: &Notifier) -> Result<bool, String> {
        let now = Instant::now();
        let recent = self.last_sent.lock().unwrap().get(&alert.turbine_id)
            .is_some_and(|t| now.duration_since(*t) < ALERT_COOLDOWN);
        if recent {
            return Ok(false);
        }
        notifier.send(alert)?;
        self.last_sent.lock().unwrap().insert(alert.turbine_id.clone(), now);
        Ok(true)
    }
}

/// Shape check for ids taken from the request path.
pub fn valid_turbine_id(id: &str) -> bool {
    !id.is_empty() && id.len() <= 64 && id.is_ascii()
}

fn shell_quote(s: &str) -> String {
    format!("'{}'", s.replace('\'', r"'\''"))
}

pub struct Notifier {
    pub curl_path: String,
}

impl Notifier {
    pub fn send(&self, alert: &Alert) -> Result<(), String> {
        let site = shell_quote(&alert.site);
        // turbine_id was already checked by valid_turbine_id() in the request handler.
        let cmd = format!(
            "{} -fsS -X POST {PAGER_URL} -d site={site} -d turbine={} -d rms={:.2} -d zone={}",
            self.curl_path, alert.turbine_id, alert.rms, alert.zone
        );
        let status = Command::new("sh").arg("-c").arg(&cmd).status().map_err(|e| e.to_string())?;
        if status.success() { Ok(()) } else { Err(format!("pager hook exited with {status}")) }
    }
}
