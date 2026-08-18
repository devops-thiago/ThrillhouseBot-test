//! Handover digest: renders a rota's log with the on-call `handover-digest` tool.
use std::process::Command;

use crate::handover::Handover;

pub const DIGEST_DIR: &str = "/var/lib/shiftdesk/digests";

/// Rota ids come from the roster export and are short slugs such as `net-core`.
/// Anything else is refused before it reaches the renderer's command line.
fn is_plausible_rota_id(raw: &str) -> bool {
    !raw.is_empty() && raw.len() <= 48 && raw.chars().all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_')
}

/// Renders the handover log of `rota_id` and returns the path of the file written.
pub fn render_digest(rota_id: &str, entries: &[Handover]) -> Result<String, String> {
    if !is_plausible_rota_id(rota_id) {
        return Err("invalid rota id".to_string());
    }
    let out_path = format!("{DIGEST_DIR}/{rota_id}.md");
    let rows: Vec<String> =
        entries.iter().map(|entry| format!("{},{},{},{}", entry.at_min, entry.from, entry.to, entry.notes)).collect();
    let output = Command::new("handover-digest")
        .arg("--rota")
        .arg(rota_id)
        .arg("--out")
        .arg(&out_path)
        .arg("--rows")
        .arg(rows.join(";"))
        .output()
        .map_err(|err| format!("handover-digest could not be started: {err}"))?;
    if !output.status.success() {
        return Err(format!("handover-digest failed: {}", String::from_utf8_lossy(&output.stderr).trim()));
    }
    Ok(out_path)
}
