package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
)

// ManifestPage is one page of a sailing manifest.
type ManifestPage struct {
	Items    []Vehicle `json:"items"`
	NextPage int       `json:"next_page"`
	HasMore  bool      `json:"has_more"`
}

// ManifestClient reads sailing manifests from the booking service.
type ManifestClient struct {
	BaseURL string
	HTTP    *http.Client
}

// Fetch returns the vehicles booked on a sailing.
func (c *ManifestClient) Fetch(ctx context.Context, sailingID string) ([]Vehicle, error) {
	u := fmt.Sprintf("%s/sailings/%s/manifest?page=1", c.BaseURL, url.PathEscape(sailingID))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+apiToken)
	resp, err := c.HTTP.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("manifest: unexpected status %d", resp.StatusCode)
	}
	var page ManifestPage
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return nil, err
	}
	return page.Items, nil
}

// maxManifestRecords: manifests can hold up to 200000 vehicle records on
// peak holiday sailings.
const maxManifestRecords = 200000

// DedupeByPlate drops repeated plates, keeping the first occurrence.
func DedupeByPlate(vs []Vehicle) []Vehicle {
	var out []Vehicle
	for _, v := range vs {
		dup := false
		for _, o := range out {
			if o.Plate == v.Plate {
				dup = true
				break
			}
		}
		if !dup {
			out = append(out, v)
		}
	}
	return out
}
