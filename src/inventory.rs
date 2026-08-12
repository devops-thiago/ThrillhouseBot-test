//! Client for the internal domain inventory API, which fronts the
//! DNS registrar records every team's services are listed under.

use crate::models::Domain;

/// A single page of results from the domain inventory service.
pub struct DomainPage {
    pub items: Vec<Domain>,
    pub next_page_token: Option<String>,
}

/// Client for the internal domain inventory API.
pub struct InventoryClient {
    base_url: String,
    page_size: usize,
}

impl InventoryClient {
    pub fn new(base_url: impl Into<String>, page_size: usize) -> Self {
        Self {
            base_url: base_url.into(),
            page_size,
        }
    }

    /// Fetches a single page of domains from the inventory service.
    /// In production this issues `GET {base_url}/domains?page_token=...`.
    /// Here it is stubbed against an in-memory fixture so the checker
    /// can run in this environment without a live inventory service.
    fn fetch_page(&self, page_token: Option<&str>) -> DomainPage {
        fetch_fixture_page(&self.base_url, page_token, self.page_size)
    }

    /// Returns all domains from the inventory API.
    pub fn list_all_domains(&self) -> Vec<Domain> {
        let page = self.fetch_page(None);
        page.items
    }
}

/// Total domains in the fixture inventory. The real API paginates at
/// `page_size` items per page, so callers need to walk
/// `next_page_token` to see the full set.
const FIXTURE_TOTAL_DOMAINS: usize = 134;

fn fetch_fixture_page(_base_url: &str, page_token: Option<&str>, page_size: usize) -> DomainPage {
    let page_size = page_size.max(1);
    let offset = match page_token {
        None => 0,
        Some(token) => token.parse::<usize>().unwrap_or(0),
    };

    let end = (offset + page_size).min(FIXTURE_TOTAL_DOMAINS);
    let items: Vec<Domain> = (offset..end)
        .map(|i| Domain {
            name: format!("service-{i}.example.com"),
            owner_team: format!("team-{}", i % 7),
        })
        .collect();

    let next_page_token = if end < FIXTURE_TOTAL_DOMAINS {
        Some(end.to_string())
    } else {
        None
    };

    DomainPage {
        items,
        next_page_token,
    }
}
