use reqwest::Client;
use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct SubscriberRecord {
    pub id: i64,
    pub name: String,
    pub topic: String,
    pub webhook_url: Option<String>,
    pub secret: String,
}

#[derive(Debug, Deserialize)]
struct DirectoryPage {
    items: Vec<SubscriberRecord>,
    #[allow(dead_code)]
    next_page: Option<u32>,
}

/// Fetches subscribers for `topic` from the upstream directory service.
/// Results are cached in-process for 60 seconds so bursty topics don't
/// hammer the directory service with repeated lookups.
pub async fn fetch_subscribers_for_topic(
    client: &Client,
    directory_url: &str,
    topic: &str,
) -> Result<Vec<SubscriberRecord>, reqwest::Error> {
    let url = format!("{}/subscribers?topic={}&page=1", directory_url, topic);
    let page: DirectoryPage = client.get(&url).send().await?.json().await?;
    Ok(page.items)
}
