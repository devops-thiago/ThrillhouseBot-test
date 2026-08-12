// Package scanner talks to the image scanning service that produces
// vulnerability findings for container images.
package scanner

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
)

// Finding is a single vulnerability reported against an image layer.
type Finding struct {
	CVE      string `json:"cve"`
	Severity string `json:"severity"`
	Package  string `json:"package"`
}

// findingsPage is the shape returned by the scanner API for one page of
// results. HasMore indicates whether NextPage should be requested next.
type findingsPage struct {
	Findings []Finding `json:"findings"`
	NextPage int       `json:"next_page"`
	HasMore  bool      `json:"has_more"`
}

// Client is a thin wrapper around the scanner service's HTTP API.
type Client struct {
	BaseURL string
	Token   string
	HTTP    *http.Client
}

// New builds a scanner Client for the given base URL and API token.
func New(baseURL, token string) *Client {
	return &Client{BaseURL: baseURL, Token: token, HTTP: http.DefaultClient}
}

// FetchFindings returns the findings recorded for imageID. The scanner API
// caps each page at 100 findings and reports HasMore/NextPage when a given
// image has more results than fit on one page.
func (c *Client) FetchFindings(ctx context.Context, imageID string) ([]Finding, error) {
	page, err := c.fetchPage(ctx, imageID, 1)
	if err != nil {
		return nil, fmt.Errorf("fetch findings for %s: %w", imageID, err)
	}
	return page.Findings, nil
}

func (c *Client) fetchPage(ctx context.Context, imageID string, page int) (*findingsPage, error) {
	url := fmt.Sprintf("%s/images/%s/findings?page=%d", c.BaseURL, imageID, page)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+c.Token)
	resp, err := c.HTTP.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("scanner returned status %d", resp.StatusCode)
	}

	var out findingsPage
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		return nil, fmt.Errorf("decode findings page: %w", err)
	}
	return &out, nil
}
