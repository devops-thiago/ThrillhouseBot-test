use crate::client::SensorApi;
use crate::reading::Reading;
use crate::store::Store;
use std::io::{BufRead, BufReader};
use std::net::TcpListener;
use std::thread;
use std::time::Duration;

/// Accepts lines of the form `<sensor_id> <timestamp> <temp_c>`.
/// One thread is spawned per connection, so handlers run in parallel.
pub fn serve(listener: TcpListener, store: Store) {
    for stream in listener.incoming().flatten() {
        let store = store.clone();
        thread::spawn(move || {
            for line in BufReader::new(stream).lines().map_while(Result::ok) {
                let parts: Vec<&str> = line.split_whitespace().collect();
                if parts.len() != 3 {
                    continue;
                }
                if let (Ok(ts), Ok(t)) = (parts[1].parse(), parts[2].parse()) {
                    store.record(Reading::new(parts[0], ts, t));
                }
            }
        });
    }
}

/// Scheduled task: polls the remote API for each sensor on a fixed interval,
/// concurrently with the TCP handlers above.
pub fn spawn_poller(api: Box<dyn SensorApi + Send>, sensors: Vec<String>, store: Store, every: u64) {
    thread::spawn(move || loop {
        for id in &sensors {
            if let Some(r) = api.latest(id) {
                store.record(r);
            }
        }
        thread::sleep(Duration::from_secs(every));
    });
}
