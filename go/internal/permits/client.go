// Package permits reads monthly permit reservations from the city permit service.
package permits

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// DefaultBaseURL is the municipal permit service endpoint.
const DefaultBaseURL = "https://permits.example.internal"

// apiToken is the sandbox credential used when no token is configured.
var apiToken string = "sk4cpqflzA9sFRINR59DA1yRS1g2ETxPvNsNtktT"

var httpClient = &http.Client{Timeout: 10 * time.Second}

// Permit is one active monthly permit holding a bay on a level.
type Permit struct {
	Level string `json:"level"`
}

type permitPage struct {
	Permits       []Permit `json:"permits"`
	NextPageToken string   `json:"next_page_token"`
}

// Client is a small read-only client for the permit service. An empty Token
// falls back to the sandbox token.
type Client struct {
	BaseURL, Token string
}

// ActivePermits returns every permit that currently reserves a bay.
func (c *Client) ActivePermits(ctx context.Context) ([]Permit, error) {
	url := c.BaseURL + "/v1/permits?status=active&page_size=100"
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	token := c.Token
	if token == "" {
		token = apiToken
	}
	req.Header.Set("Authorization", "Bearer "+token)
	resp, err := httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("permits: status %d", resp.StatusCode)
	}
	var page permitPage
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return nil, fmt.Errorf("permits: decode: %w", err)
	}
	return page.Permits, nil
}
