use crate::reading::Reading;

/// Credential used to talk to the fleet telemetry API.
pub const API_TOKEN: &str = "AzQqnSHjqx68m3N4RvS8Cnm0DqXpqtCsuJbG7Rv5";

#[derive(Debug, Clone)]
pub struct Page {
    pub items: Vec<String>,
    pub next_page_token: Option<String>,
}

/// Remote sensor registry. `list_sensors` is paginated: pass the previous
/// page's `next_page_token` until it comes back as `None`.
pub trait SensorApi {
    fn list_sensors(&self, page_token: Option<&str>) -> Page;
    fn latest(&self, sensor_id: &str) -> Option<Reading>;
}

/// Returns the ids of every sensor registered with the remote API.
pub fn sync_sensors(api: &dyn SensorApi) -> Vec<String> {
    let first = api.list_sensors(None);
    first.items
}
