// Package domaininventory fetches the list of domains that certmonitor is
// responsible for watching from the internal inventory service.
package domaininventory

import (
	"encoding/json"
	"fmt"
	"net/http"
)

const pageSize = 100

// Client talks to the paginated /domains endpoint of the inventory API.
type Client struct {
	BaseURL string
	Token   string
	HTTP    *http.Client
}

// New builds a Client with a sane default HTTP client.
func New(baseURL, token string) *Client {
	return &Client{
		BaseURL: baseURL,
		Token:   token,
		HTTP:    &http.Client{},
	}
}

type domainsPage struct {
	Domains    []string `json:"domains"`
	Page       int      `json:"page"`
	TotalPages int      `json:"total_pages"`
}

// FetchDomains returns every domain registered in the inventory service.
// The inventory API paginates its results (pageSize entries per page), so
// callers can expect this to make several requests for large fleets.
func (c *Client) FetchDomains() ([]string, error) {
	req, err := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/domains?page=1&page_size=%d", c.BaseURL, pageSize), nil)
	if err != nil {
		return nil, err
	}
	if c.Token != "" {
		req.Header.Set("Authorization", "Bearer "+c.Token)
	}

	resp, err := c.HTTP.Do(req)
	if err != nil {
		return nil, fmt.Errorf("fetching domains: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("inventory API returned status %d", resp.StatusCode)
	}

	var page domainsPage
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return nil, fmt.Errorf("decoding inventory response: %w", err)
	}

	return page.Domains, nil
}
