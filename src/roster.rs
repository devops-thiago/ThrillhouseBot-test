//! Client for the staffing roster export, and the cache the HTTP handlers read.
use std::io::{Read, Write};
use std::net::TcpStream;
use std::sync::{Arc, Mutex};

/// Upper bound on the rows one sync pass keeps in memory. The export carries one
/// row per rota and one per absence; the largest tenant we mirror answers with
/// about 400 rotas and a few thousand absences, so the cap is generous.
pub const MAX_SYNC_ROWS: usize = 20_000;

/// A rota is a fixed member order and a shift length: whoever sits at position
/// `n` of the order holds shift `n` of the cycle.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Rota {
    pub id: String,
    pub members: Vec<String>,
    pub shift_len_min: u64,
    /// Minute the first shift of the cycle started, counted from the Unix epoch.
    pub anchor_min: u64,
}

/// A window in which an engineer cannot take a shift: leave, training, sickness.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Absence {
    pub engineer: String,
    pub from_min: u64,
    pub to_min: u64,
}

impl Absence {
    pub fn covers(&self, at_min: u64) -> bool {
        self.from_min <= at_min && at_min < self.to_min
    }
}

/// Source of the rotas and absences the rotation is computed against.
pub trait RotaDirectory: Send + Sync {
    fn list_rotas(&self) -> Vec<Rota>;
    fn list_absences(&self) -> Vec<Absence>;
}

/// What the sync thread mirrored on its last successful pass. Cloning shares the
/// same state, so the HTTP handlers always read the latest pass.
#[derive(Clone, Default)]
pub struct RotaCache {
    state: Arc<Mutex<(Vec<Rota>, Vec<Absence>)>>,
}

impl RotaCache {
    pub fn replace(&self, rotas: Vec<Rota>, absences: Vec<Absence>) {
        *self.state.lock().expect("rota cache poisoned") = (rotas, absences);
    }

    pub fn rota(&self, rota_id: &str) -> Option<Rota> {
        self.state.lock().expect("rota cache poisoned").0.iter().find(|rota| rota.id == rota_id).cloned()
    }
}

impl RotaDirectory for RotaCache {
    fn list_rotas(&self) -> Vec<Rota> {
        self.state.lock().expect("rota cache poisoned").0.clone()
    }

    fn list_absences(&self) -> Vec<Absence> {
        self.state.lock().expect("rota cache poisoned").1.clone()
    }
}

/// Plain HTTP/1.1 GET against the `host:port` roster endpoint.
fn get(endpoint: &str, path: &str) -> Result<String, String> {
    let mut stream = TcpStream::connect(endpoint).map_err(|e| e.to_string())?;
    let head = format!("GET {path} HTTP/1.1\r\nHost: {endpoint}\r\nConnection: close\r\n\r\n");
    stream.write_all(head.as_bytes()).map_err(|e| e.to_string())?;
    let mut raw = String::new();
    stream.read_to_string(&mut raw).map_err(|e| e.to_string())?;
    Ok(raw.split("\r\n\r\n").nth(1).unwrap_or("").to_string())
}

/// The export answers with a `# key: value` header block — `# page_size: 1000`,
/// `# next_page: 2` — followed by one CSV row per record.
pub fn header_value<'a>(body: &'a str, key: &str) -> Option<&'a str> {
    let marker = format!("# {key}:");
    body.lines().find_map(|line| line.trim().strip_prefix(&marker)).map(str::trim)
}

fn data_rows(body: &str) -> impl Iterator<Item = Vec<&str>> + '_ {
    body.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| line.split(',').map(str::trim).collect::<Vec<&str>>())
}

/// Rota rows are `rota_id,shift_len_min,anchor_min,member|member|...`. A rota with
/// nobody on it is kept: the coverage scan is what reports it, not the parser.
pub fn parse_rotas(body: &str) -> Vec<Rota> {
    data_rows(body)
        .filter(|cols| cols.len() >= 4)
        .filter_map(|cols| {
            Some(Rota {
                id: cols[0].to_string(),
                shift_len_min: cols[1].parse().ok()?,
                anchor_min: cols[2].parse().ok()?,
                members: cols[3].split('|').map(str::trim).filter(|m| !m.is_empty()).map(str::to_string).collect(),
            })
        })
        .collect()
}

/// Absence rows are `engineer,from_min,to_min`.
pub fn parse_absences(body: &str) -> Vec<Absence> {
    data_rows(body)
        .filter(|cols| cols.len() >= 3)
        .filter_map(|cols| {
            Some(Absence {
                engineer: cols[0].to_string(),
                from_min: cols[1].parse().ok()?,
                to_min: cols[2].parse().ok()?,
            })
        })
        .filter(|absence| absence.from_min < absence.to_min)
        .collect()
}

pub struct RosterClient {
    pub endpoint: String,
}

impl RosterClient {
    pub fn fetch_rotas(&self) -> Result<Vec<Rota>, String> {
        let body = get(&self.endpoint, "/api/v1/rotas?page=1")?;
        let mut rotas = parse_rotas(&body);
        if let Some(next_page) = header_value(&body, "next_page") {
            eprintln!("roster: {} rota rows, further pages start at {next_page}", rotas.len());
        }
        rotas.truncate(MAX_SYNC_ROWS);
        Ok(rotas)
    }

    pub fn fetch_absences(&self) -> Result<Vec<Absence>, String> {
        let body = get(&self.endpoint, "/api/v1/absences")?;
        let mut absences = parse_absences(&body);
        absences.truncate(MAX_SYNC_ROWS);
        Ok(absences)
    }
}
