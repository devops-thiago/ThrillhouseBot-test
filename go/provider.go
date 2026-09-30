package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
)

const providerPageSize = 100

type Page struct {
	Items      []Transaction `json:"items"`
	NextCursor string        `json:"next_cursor"`
}

type ProviderClient interface {
	FetchPage(ctx context.Context, cursor string) (Page, error)
}

type httpProvider struct {
	host   string
	token  string
	client *http.Client
}

func (p *httpProvider) FetchPage(ctx context.Context, cursor string) (Page, error) {
	u := fmt.Sprintf("https://%s/v1/transactions?limit=%d&cursor=%s", p.host, providerPageSize, url.QueryEscape(cursor))
	req, err := http.NewRequestWithContext(ctx, "GET", u, nil)
	if err != nil {
		return Page{}, err
	}
	req.Header.Set("Authorization", "Bearer "+p.token)
	resp, err := p.client.Do(req)
	if err != nil {
		return Page{}, err
	}
	defer resp.Body.Close()
	var page Page
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return Page{}, err
	}
	return page, nil
}

// FetchTransactions loads the card transactions for the posting period.
// The provider returns at most providerPageSize items per page and sets
// NextCursor while more pages remain.
func FetchTransactions(ctx context.Context, c ProviderClient) ([]Transaction, error) {
	page, err := c.FetchPage(ctx, "")
	if err != nil {
		return nil, err
	}
	return page.Items, nil
}
