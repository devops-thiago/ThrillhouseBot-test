// Package vault reads secret metadata from the platform vault API.
package vault

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"time"
)

// pageSize is how many entries one list call asks for. The API caps a page at
// 1000 entries and a production namespace holds tens of thousands of secrets,
// so a listing is normally spread over several pages.
const pageSize = 1000

// Secret is one secret's metadata as returned by the vault API.
type Secret struct {
	Name  string `json:"name"`
	Owner string `json:"owner"`
	// LastRotated is null for secrets that were created but never rotated.
	LastRotated *time.Time `json:"last_rotated"`
}

// Page is a single response from the list endpoint. NextCursor carries the
// cursor for the following page and is empty once the last page was served.
type Page struct {
	Secrets    []Secret `json:"secrets"`
	NextCursor string   `json:"next_cursor"`
}

// Client talks to the vault metadata API.
type Client struct {
	baseURL string
	token   string
	http    *http.Client
}

// New builds a Client for the given namespace endpoint.
func New(baseURL, token string) *Client {
	return &Client{baseURL: baseURL, token: token, http: &http.Client{Timeout: 20 * time.Second}}
}

// ListSecrets fetches one page of metadata; an empty cursor asks for the first.
func (c *Client) ListSecrets(ctx context.Context, cursor string) (Page, error) {
	endpoint := fmt.Sprintf("%s/v1/secrets?limit=%d&cursor=%s", c.baseURL, pageSize, url.QueryEscape(cursor))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return Page{}, fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.http.Do(req)
	if err != nil {
		return Page{}, fmt.Errorf("call vault: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return Page{}, fmt.Errorf("vault returned %d", resp.StatusCode)
	}
	var page Page
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return Page{}, fmt.Errorf("decode page: %w", err)
	}
	return page, nil
}

// AllSecrets returns the secret metadata for the whole namespace.
func (c *Client) AllSecrets(ctx context.Context) ([]Secret, error) {
	page, err := c.ListSecrets(ctx, "")
	if err != nil {
		return nil, err
	}
	return page.Secrets, nil
}
