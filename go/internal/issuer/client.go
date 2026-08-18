// Package issuer reads the certificates the internal CA has issued.
package issuer

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

// Certificate is one issued certificate as the CA reports it.
type Certificate struct {
	Serial   string    `json:"serial"`
	Subject  string    `json:"subject"`
	NotAfter time.Time `json:"not_after"`
	// SANs carries the subject alternative names; a certificate covers an
	// endpoint when either the subject or one of the SANs matches its host.
	SANs    []string `json:"sans"`
	Revoked bool     `json:"revoked"`
}

// Client talks to the CA's certificate listing API.
type Client struct {
	baseURL string
	token   string
	http    *http.Client
}

// New builds a Client for the given CA endpoint.
func New(baseURL, token string) *Client {
	return &Client{
		baseURL: strings.TrimSuffix(baseURL, "/"),
		token:   token,
		http:    &http.Client{Timeout: 30 * time.Second},
	}
}

// Issued returns every certificate the CA currently considers issued. The CA
// keeps superseded certificates in the listing until they expire, so a host
// that has just been renewed appears more than once.
func (c *Client) Issued(ctx context.Context) ([]Certificate, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+"/v1/certificates?state=issued", nil)
	if err != nil {
		return nil, fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.http.Do(req)
	if err != nil {
		return nil, fmt.Errorf("call issuer: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("issuer returned %d", resp.StatusCode)
	}
	var body struct {
		Certificates []Certificate `json:"certificates"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return nil, fmt.Errorf("decode certificates: %w", err)
	}
	return body.Certificates, nil
}
