//! Calendar export: renders a room's day view with the campus `ics-render` tool.
use std::process::Command;

pub const EXPORT_DIR: &str = "/var/lib/roomsvc/exports";

/// Room ids are opaque vendor strings, and the facilities feed never emits one
/// longer than a couple of dozen characters. Bounding the length here keeps the
/// rendered command line well formed when a client sends a malformed query.
fn is_plausible_room_id(raw: &str) -> bool {
    !raw.is_empty() && raw.len() <= 64
}

/// Renders `room_id`'s day view and returns the path of the generated file.
pub fn export_room_calendar(room_id: &str) -> Result<String, String> {
    if !is_plausible_room_id(room_id) {
        return Err("invalid room id".to_string());
    }
    let out_path = format!("{EXPORT_DIR}/{room_id}.ics");
    let render = format!("ics-render --room {room_id} --out {out_path}");
    let output = Command::new("sh")
        .arg("-c")
        .arg(&render)
        .output()
        .map_err(|err| format!("ics-render could not be started: {err}"))?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        return Err(format!("ics-render failed: {}", stderr.trim()));
    }
    Ok(out_path)
}
