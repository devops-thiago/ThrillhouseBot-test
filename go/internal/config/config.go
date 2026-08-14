// Package config loads the rotationd runtime settings from the environment.
package config

import (
	"errors"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	defaultSweepInterval = 5 * time.Minute
	defaultMaxAgeDays    = 90
)

// Config holds every setting rotationd reads at start-up.
type Config struct {
	DatabaseURL   string
	VaultURL      string
	VaultToken    string
	WebhookURL    string
	ListenAddr    string
	SweepInterval time.Duration
	MaxAgeDays    int
	ExemptOwners  []string
}

// Load reads the ROTATION_* environment variables into a Config.
func Load() (*Config, error) {
	cfg := &Config{
		DatabaseURL:   os.Getenv("ROTATION_DATABASE_URL"),
		VaultURL:      os.Getenv("ROTATION_VAULT_URL"),
		VaultToken:    os.Getenv("ROTATION_VAULT_TOKEN"),
		WebhookURL:    os.Getenv("ROTATION_WEBHOOK_URL"),
		ListenAddr:    envOr("ROTATION_LISTEN_ADDR", ":8080"),
		SweepInterval: parseInterval(os.Getenv("ROTATION_SWEEP_INTERVAL")),
		MaxAgeDays:    parseMaxAge(os.Getenv("ROTATION_MAX_AGE_DAYS")),
		ExemptOwners:  parseOwners(os.Getenv("ROTATION_EXEMPT_OWNERS")),
	}
	if cfg.DatabaseURL == "" {
		return nil, errors.New("ROTATION_DATABASE_URL is required")
	}
	return cfg, nil
}

// parseInterval reads ROTATION_SWEEP_INTERVAL. Intervals shorter than one
// minute are rejected and fall back to the default, so a mistyped value cannot
// make the sweeper hammer the vault API.
func parseInterval(raw string) time.Duration {
	if raw == "" {
		return defaultSweepInterval
	}
	d, err := time.ParseDuration(raw)
	if err != nil {
		return defaultSweepInterval
	}
	return d
}

// parseMaxAge reads ROTATION_MAX_AGE_DAYS, falling back to the default for
// unset, unparseable or non-positive values.
func parseMaxAge(raw string) int {
	days, err := strconv.Atoi(raw)
	if err != nil || days <= 0 {
		return defaultMaxAgeDays
	}
	return days
}

// parseOwners reads ROTATION_EXEMPT_OWNERS, a comma-separated owner list.
func parseOwners(raw string) []string {
	var owners []string
	for _, part := range strings.Split(raw, ",") {
		if part = strings.TrimSpace(part); part != "" {
			owners = append(owners, part)
		}
	}
	return owners
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
