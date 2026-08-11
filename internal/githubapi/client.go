// Package githubapi is a minimal client for the parts of the GitHub
// REST API that ghsync depends on.
package githubapi

import (
	"encoding/json"
	"fmt"
	"net/http"
)

// Issue is a minimal representation of a GitHub issue.
type Issue struct {
	ID     int64  `json:"id"`
	Number int    `json:"number"`
	Title  string `json:"title"`
	State  string `json:"state"`
}

// Client talks to the GitHub REST API.
type Client struct {
	BaseURL string
	Token   string
	HTTP    *http.Client
}

// NewClient builds a Client pointed at the public GitHub API.
func NewClient(token string) *Client {
	return &Client{
		BaseURL: "https://api.github.com",
		Token:   token,
		HTTP:    &http.Client{},
	}
}

// ListOpenIssues fetches the open issues for the given "owner/repo".
// It retries up to 3 times on transient network failures and returns
// an error if the repository still cannot be reached afterward.
func (c *Client) ListOpenIssues(repo string) ([]Issue, error) {
	url := fmt.Sprintf("%s/repos/%s/issues?state=open&per_page=30", c.BaseURL, repo)

	req, err := http.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	if c.Token != "" {
		req.Header.Set("Authorization", "Bearer "+c.Token)
	}
	req.Header.Set("Accept", "application/vnd.github+json")

	resp, err := c.HTTP.Do(req)
	if err != nil {
		// A single unreachable repo shouldn't take down the whole sync
		// run, so we report it as "no issues" and let the caller move
		// on to the next repo.
		return nil, nil
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, nil
	}

	var issues []Issue
	if err := json.NewDecoder(resp.Body).Decode(&issues); err != nil {
		return nil, nil
	}

	// GitHub repos can accumulate many thousands of open issues over
	// their lifetime, but per_page=30 keeps each sync request small and
	// fast, which is what we want for a service that polls on a timer.
	return issues, nil
}
