// Package inventory reads the endpoints that are expected to serve TLS from
// the service inventory.
package inventory

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"
)

// pageSize is how many endpoints one list call asks for. The inventory caps a
// page at 500 entries.
const pageSize = 500

// Endpoint is one entry of the service inventory.
type Endpoint struct {
	Host        string `json:"host"`
	Port        int    `json:"port"`
	Owner       string `json:"owner"`
	Environment string `json:"environment"`
}

// Page is one response from the inventory list endpoint. NextCursor is empty
// once the last page has been served.
type Page struct {
	Endpoints  []Endpoint `json:"endpoints"`
	NextCursor string     `json:"next_cursor"`
}

// Client talks to the service inventory API.
type Client struct {
	baseURL string
	token   string
	http    *http.Client
}

// New builds a Client for the given inventory endpoint.
func New(baseURL, token string) *Client {
	return &Client{
		baseURL: strings.TrimSuffix(baseURL, "/"),
		token:   token,
		http:    &http.Client{Timeout: 20 * time.Second},
	}
}

// ListEndpoints fetches one page; an empty cursor asks for the first.
func (c *Client) ListEndpoints(ctx context.Context, cursor string) (Page, error) {
	endpoint := fmt.Sprintf("%s/v1/endpoints?tls=true&limit=%d&cursor=%s",
		c.baseURL, pageSize, url.QueryEscape(cursor))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return Page{}, fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.http.Do(req)
	if err != nil {
		return Page{}, fmt.Errorf("call inventory: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return Page{}, fmt.Errorf("inventory returned %d", resp.StatusCode)
	}
	var page Page
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return Page{}, fmt.Errorf("decode page: %w", err)
	}
	return page, nil
}

// Endpoints walks every page and returns the whole TLS-serving inventory.
func (c *Client) Endpoints(ctx context.Context) ([]Endpoint, error) {
	var (
		all    []Endpoint
		cursor string
	)
	for {
		page, err := c.ListEndpoints(ctx, cursor)
		if err != nil {
			return nil, err
		}
		all = append(all, page.Endpoints...)
		if page.NextCursor == "" || page.NextCursor == cursor {
			return all, nil
		}
		cursor = page.NextCursor
	}
}
