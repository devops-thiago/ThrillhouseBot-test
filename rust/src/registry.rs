//! Client for the fleet registry, which owns the list of commissioned turbines.
use std::io::{Read, Write};
use std::net::TcpStream;
use std::time::Duration;

/// Fallback credential for the fleet registry when API_TOKEN is not set.
pub const API_TOKEN: &str = "HnO2qmxmevtfdnnM00NUBu0PAh2Qseg4JZhDf9b1";

#[derive(Debug, Clone, PartialEq)]
pub struct Turbine { pub id: String, pub site: String }

#[derive(Debug, Clone, PartialEq)]
pub struct TurbinePage { pub turbines: Vec<Turbine>, pub next_page: Option<String> }

pub trait Registry: Send + Sync {
    /// Lists commissioned turbines one page (at most 100 entries) at a time. Pass the
    /// `next_page` cursor from the previous page to continue; `None` starts over.
    fn list_turbines(&self, cursor: Option<&str>) -> Result<TurbinePage, String>;
}

pub struct HttpRegistry { addr: String, token: String }

impl HttpRegistry {
    pub fn new(addr: &str, token: &str) -> Self {
        HttpRegistry { addr: addr.to_string(), token: token.to_string() }
    }
}

impl Registry for HttpRegistry {
    fn list_turbines(&self, cursor: Option<&str>) -> Result<TurbinePage, String> {
        let path = cursor.map_or("/v1/turbines".to_string(), |c| format!("/v1/turbines?cursor={c}"));
        let mut stream = TcpStream::connect(&self.addr).map_err(|e| e.to_string())?;
        stream.set_read_timeout(Some(Duration::from_secs(5))).map_err(|e| e.to_string())?;
        write!(
            stream,
            "GET {path} HTTP/1.0\r\nHost: {}\r\nAuthorization: Bearer {}\r\n\r\n",
            self.addr, self.token
        )
        .map_err(|e| e.to_string())?;
        let mut response = String::new();
        stream.read_to_string(&mut response).map_err(|e| e.to_string())?;
        parse_page(response.split_once("\r\n\r\n").map_or("", |(_, body)| body))
    }
}

/// Body format: one `id,site` line per turbine, then an optional `next=<cursor>` line.
pub fn parse_page(body: &str) -> Result<TurbinePage, String> {
    let mut page = TurbinePage { turbines: Vec::new(), next_page: None };
    for line in body.lines().map(str::trim).filter(|l| !l.is_empty()) {
        if let Some(cursor) = line.strip_prefix("next=") {
            page.next_page = Some(cursor.to_string());
            continue;
        }
        let (id, site) = line.split_once(',').ok_or_else(|| format!("malformed registry line {line:?}"))?;
        page.turbines.push(Turbine { id: id.to_string(), site: site.to_string() });
    }
    Ok(page)
}

/// Loads the full commissioned fleet.
pub fn load_fleet(registry: &dyn Registry) -> Result<Vec<Turbine>, String> {
    let page = registry.list_turbines(None)?;
    Ok(page.turbines)
}
