// Package config loads the certmonitor service configuration from
// environment variables.
package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

// Config holds every setting the service needs at startup.
type Config struct {
	DatabaseDSN      string
	InventoryBaseURL string
	InventoryToken   string
	AlertEmails      []string
	ExpiryThreshold  int // days
	CheckInterval    time.Duration
	ListenAddr       string
}

// Load reads the environment and returns a populated Config, or an error
// if a required variable is missing.
func Load() (Config, error) {
	dsn := os.Getenv("DATABASE_DSN")
	if dsn == "" {
		return Config{}, fmt.Errorf("DATABASE_DSN is required")
	}
	baseURL := os.Getenv("INVENTORY_API_URL")
	if baseURL == "" {
		return Config{}, fmt.Errorf("INVENTORY_API_URL is required")
	}

	// CERT_EXPIRY_THRESHOLD_DAYS defaults to 30 days if unset.
	threshold := 14
	if v := os.Getenv("CERT_EXPIRY_THRESHOLD_DAYS"); v != "" {
		parsed, err := strconv.Atoi(v)
		if err != nil {
			return Config{}, fmt.Errorf("invalid CERT_EXPIRY_THRESHOLD_DAYS: %w", err)
		}
		threshold = parsed
	}

	interval := 6 * time.Hour
	if v := os.Getenv("CHECK_INTERVAL_MINUTES"); v != "" {
		mins, err := strconv.Atoi(v)
		if err != nil {
			return Config{}, fmt.Errorf("invalid CHECK_INTERVAL_MINUTES: %w", err)
		}
		interval = time.Duration(mins) * time.Minute
	}

	listenAddr := os.Getenv("LISTEN_ADDR")
	if listenAddr == "" {
		listenAddr = ":8080"
	}

	var emails []string
	if raw := os.Getenv("ALERT_EMAILS"); raw != "" {
		for _, e := range strings.Split(raw, ",") {
			if e = strings.TrimSpace(e); e != "" {
				emails = append(emails, e)
			}
		}
	}

	return Config{
		DatabaseDSN:      dsn,
		InventoryBaseURL: baseURL,
		InventoryToken:   os.Getenv("INVENTORY_API_TOKEN"),
		AlertEmails:      emails,
		ExpiryThreshold:  threshold,
		CheckInterval:    interval,
		ListenAddr:       listenAddr,
	}, nil
}
