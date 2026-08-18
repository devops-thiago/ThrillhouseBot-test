// Package config loads the certwatchd runtime settings from the environment.
package config

import (
	"errors"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	defaultScanInterval = 30 * time.Minute
	defaultWarnDays     = 30
	defaultCriticalDays = 7
	defaultListenAddr   = ":8080"
)

// Config holds every setting certwatchd reads at start-up.
type Config struct {
	DatabaseURL  string
	InventoryURL string
	IssuerURL    string
	DigestURL    string
	APIToken     string
	ListenAddr   string
	ScanInterval time.Duration
	WarnDays     int
	CriticalDays int
	SkipOwners   []string
}

// Load reads the CERTWATCH_* environment variables into a Config. Only the
// database URL is mandatory; everything else has a working default or is
// allowed to stay empty (an empty digest URL disables delivery).
func Load() (*Config, error) {
	cfg := &Config{
		DatabaseURL:  os.Getenv("CERTWATCH_DATABASE_URL"),
		InventoryURL: os.Getenv("CERTWATCH_INVENTORY_URL"),
		IssuerURL:    os.Getenv("CERTWATCH_ISSUER_URL"),
		DigestURL:    os.Getenv("CERTWATCH_DIGEST_URL"),
		APIToken:     os.Getenv("CERTWATCH_API_TOKEN"),
		ListenAddr:   envOr("CERTWATCH_LISTEN_ADDR", defaultListenAddr),
		ScanInterval: parseInterval(os.Getenv("CERTWATCH_SCAN_INTERVAL")),
		WarnDays:     parseDays(os.Getenv("CERTWATCH_WARN_DAYS"), defaultWarnDays),
		CriticalDays: parseDays(os.Getenv("CERTWATCH_CRITICAL_DAYS"), defaultCriticalDays),
		SkipOwners:   parseList(os.Getenv("CERTWATCH_SKIP_OWNERS")),
	}
	if cfg.DatabaseURL == "" {
		return nil, errors.New("CERTWATCH_DATABASE_URL is required")
	}
	if cfg.CriticalDays > cfg.WarnDays {
		return nil, errors.New("CERTWATCH_CRITICAL_DAYS must not exceed CERTWATCH_WARN_DAYS")
	}
	return cfg, nil
}

// parseInterval reads CERTWATCH_SCAN_INTERVAL as a Go duration string.
// Unparseable and non-positive values fall back to the default so a typo
// cannot turn the scanner into a busy loop against the issuer API.
func parseInterval(raw string) time.Duration {
	d, err := time.ParseDuration(strings.TrimSpace(raw))
	if err != nil || d <= 0 {
		return defaultScanInterval
	}
	return d
}

// parseDays reads a whole number of days, falling back to fallback for unset,
// unparseable or negative values.
func parseDays(raw string, fallback int) int {
	days, err := strconv.Atoi(strings.TrimSpace(raw))
	if err != nil || days < 0 {
		return fallback
	}
	return days
}

// parseList splits a comma-separated setting and trims the entries.
func parseList(raw string) []string {
	var out []string
	for _, part := range strings.Split(raw, ",") {
		if part = strings.TrimSpace(part); part != "" {
			out = append(out, part)
		}
	}
	return out
}

func envOr(key, fallback string) string {
	if v := strings.TrimSpace(os.Getenv(key)); v != "" {
		return v
	}
	return fallback
}
