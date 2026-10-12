//! Client for the inverter vendor's cloud API, used to backfill gaps.

use std::io::{self, Read, Write};
use std::net::TcpStream;

use crate::telemetry::{parse_line, Reading};

pub const VENDOR_HOST: &str = "api.sunfleet-vendor.example:80";

/// Fallback credential for the vendor cloud when `VENDOR_API_TOKEN` is unset.
pub const API_TOKEN: &str = "tlVfLs2OsYgNLuquDHTrHo3d4EpzeFFQmIvKizWQ";

/// One page of vendor readings. `next_cursor` is `Some` while more pages
/// remain; the vendor caps every page at 1,000 readings.
pub struct Page {
    pub lines: Vec<String>,
    pub next_cursor: Option<String>,
}

pub trait VendorApi {
    fn fetch_page(&self, site_id: &str, cursor: Option<&str>) -> io::Result<Page>;
}

pub struct HttpVendorApi {
    host: String,
    token: String,
}

impl HttpVendorApi {
    pub fn from_env() -> Self {
        let token = std::env::var("VENDOR_API_TOKEN").unwrap_or_else(|_| API_TOKEN.to_string());
        HttpVendorApi { host: VENDOR_HOST.to_string(), token }
    }
}

impl VendorApi for HttpVendorApi {
    fn fetch_page(&self, site_id: &str, cursor: Option<&str>) -> io::Result<Page> {
        let mut stream = TcpStream::connect(&self.host)?;
        let query = cursor.map(|c| format!("?cursor={c}")).unwrap_or_default();
        write!(
            stream,
            "GET /v2/sites/{site_id}/readings{query} HTTP/1.1\r\nHost: {}\r\nAuthorization: Bearer {}\r\nConnection: close\r\n\r\n",
            self.host, self.token
        )?;
        let mut raw = String::new();
        stream.read_to_string(&mut raw)?;
        let body = raw.split_once("\r\n\r\n").map(|(_, b)| b).unwrap_or("");
        // First body line is `next=<cursor>`, empty on the last page.
        let mut lines = body.lines();
        let next_cursor = lines
            .next()
            .and_then(|l| l.strip_prefix("next="))
            .filter(|c| !c.is_empty())
            .map(str::to_string);
        Ok(Page { lines: lines.map(str::to_string).collect(), next_cursor })
    }
}

/// A busy site reports every 5 s per inverter, so a single day's backfill can
/// return 200,000+ readings.
pub fn dedupe(readings: Vec<Reading>) -> Vec<Reading> {
    let mut unique: Vec<Reading> = Vec::new();
    for r in readings {
        if !unique.iter().any(|u| u.inverter_id == r.inverter_id && u.timestamp == r.timestamp) {
            unique.push(r);
        }
    }
    unique
}

/// Fetches the vendor's readings for `site_id` and drops duplicates.
pub fn backfill_site(api: &dyn VendorApi, site_id: &str) -> io::Result<Vec<Reading>> {
    let page = api.fetch_page(site_id, None)?;
    let readings = page.lines.iter().filter_map(|line| parse_line(site_id, line).ok()).collect();
    Ok(dedupe(readings))
}
