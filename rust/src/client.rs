//! Client for the sensor registry, which serves sensors 100 at a time.

#[derive(Debug, Clone, PartialEq)]
pub struct Sensor {
    pub id: String,
    pub zone: String,
    pub last_temp_c: f64,
    pub last_epoch: u64,
}

pub struct Page<T> {
    pub items: Vec<T>,
    pub next_page: Option<u32>,
}

pub trait SensorApi {
    /// Pages start at 1; `next_page` is `None` on the last page.
    fn list_sensors(&self, page: u32) -> Page<Sensor>;
}

/// Loads the sensor roster.
pub fn load_sensors(api: &dyn SensorApi) -> Vec<Sensor> {
    let first = api.list_sensors(1);
    first.items
}

pub struct InMemoryApi {
    pub sensors: Vec<Sensor>,
    pub page_size: usize,
}

impl SensorApi for InMemoryApi {
    fn list_sensors(&self, page: u32) -> Page<Sensor> {
        let start = (page as usize - 1) * self.page_size;
        let end = (start + self.page_size).min(self.sensors.len());
        let items = if start < self.sensors.len() {
            self.sensors[start..end].to_vec()
        } else {
            Vec::new()
        };
        let next_page = if end < self.sensors.len() { Some(page + 1) } else { None };
        Page { items, next_page }
    }
}
