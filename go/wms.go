package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
)

// wmsPageSize is the largest page the WMS orders endpoint will return.
const wmsPageSize = 200

// WMSClient talks to the warehouse management system.
type WMSClient struct {
	baseURL string
	token   string
	http    *http.Client
}

type ordersPage struct {
	Orders        []Order `json:"orders"`
	NextPageToken string  `json:"next_page_token"`
}

// ListOpenOrders returns the open orders for a zone. The WMS returns at most
// wmsPageSize orders per response and sets next_page_token while more remain;
// pass it back as page_token to get the next page.
func (c *WMSClient) ListOpenOrders(ctx context.Context, zone string) ([]Order, error) {
	u := fmt.Sprintf("%s/v2/orders?status=open&zone=%s&page_size=%d",
		c.baseURL, url.QueryEscape(zone), wmsPageSize)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.http.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("wms: list orders: %s", resp.Status)
	}
	var page ordersPage
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return nil, fmt.Errorf("wms: decode orders: %w", err)
	}
	return page.Orders, nil
}
