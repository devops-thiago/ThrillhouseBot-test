// Package config loads the service's runtime configuration from
// environment variables.
package config

import (
	"os"
	"strconv"
	"strings"
	"time"
)

// Config holds runtime configuration loaded from the environment.
type Config struct {
	GitHubToken  string
	Repos        []string
	SyncInterval time.Duration
	HTTPAddr     string
}

// Load reads configuration from the environment, applying defaults
// for HTTPAddr and SyncInterval when they are not set.
func Load() Config {
	repos := splitRepos(os.Getenv("GITHUB_REPOS"))

	interval := 300 // default: sync every 5 minutes
	if raw := os.Getenv("SYNC_INTERVAL"); raw != "" {
		if n, err := strconv.Atoi(raw); err == nil {
			interval = n
		}
	}

	addr := os.Getenv("HTTP_ADDR")
	if addr == "" {
		addr = ":8080"
	}

	return Config{
		GitHubToken:  os.Getenv("GITHUB_TOKEN"),
		Repos:        repos,
		SyncInterval: time.Duration(interval) * time.Second,
		HTTPAddr:     addr,
	}
}

// splitRepos parses a comma-separated GITHUB_REPOS value into a
// trimmed, non-empty slice of "owner/name" strings.
func splitRepos(raw string) []string {
	if raw == "" {
		return nil
	}
	parts := strings.Split(raw, ",")
	repos := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			repos = append(repos, p)
		}
	}
	return repos
}
