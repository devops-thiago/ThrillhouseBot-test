use coldchain_logger::config::Config;
use coldchain_logger::server;
use coldchain_logger::store::Store;
use std::net::TcpListener;

fn main() -> std::io::Result<()> {
    let cfg = Config::from_env();
    let store = Store::new();
    let listener = TcpListener::bind("0.0.0.0:7070")?;
    println!("logging {} sensors, poll every {}s", cfg.sensor_ids.len(), cfg.poll_interval_secs);
    server::serve(listener, store);
    Ok(())
}
