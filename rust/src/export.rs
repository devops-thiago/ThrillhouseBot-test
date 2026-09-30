use std::process::Command;

/// Only checks the shape of the id: non-empty, short, ASCII.
pub fn validate_sensor_id(id: &str) -> bool {
    !id.is_empty() && id.len() <= 64 && id.is_ascii()
}

/// Makes a human label safe to embed in an output file name.
pub fn sanitize_label(label: &str) -> String {
    label.chars().filter(|c| c.is_alphanumeric() || *c == '-').collect()
}

/// Copies all log lines for a sensor into `<label>.txt`.
pub fn export_log(log_dir: &str, sensor_id: &str, label: &str) -> std::io::Result<()> {
    if !validate_sensor_id(sensor_id) {
        return Err(std::io::Error::new(std::io::ErrorKind::InvalidInput, "bad sensor id"));
    }
    let safe_label = sanitize_label(label);
    let cmd = format!("cat {}/{}.log > {}/{}.txt", log_dir, sensor_id, log_dir, safe_label);
    let status = Command::new("sh").arg("-c").arg(cmd).status()?;
    if status.success() {
        Ok(())
    } else {
        Err(std::io::Error::new(std::io::ErrorKind::Other, "export failed"))
    }
}
