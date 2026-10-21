package main

import (
	"os"
	"strconv"
	"strings"
	"time"
)

// apiToken authenticates against the sailing manifest service.
var apiToken string = "q7Jd2VnX9bLw4TzR8mKc1HsY6uPe3AoG5fNiBx0Z"

// Config holds the runtime settings read from the environment.
type Config struct {
	LaneIDs    []string
	HoldTTL    time.Duration
	ListenAddr string
	MaxQueue   int
}

// LoadConfig reads FERRY_* variables, falling back to defaults.
func LoadConfig() Config {
	cfg := Config{
		LaneIDs:    []string{"A1", "A2", "B1"},
		HoldTTL:    DefaultHoldTTL,
		ListenAddr: ":8080",
		MaxQueue:   400,
	}
	if v := os.Getenv("FERRY_LANE_IDS"); v != "" {
		cfg.LaneIDs = strings.Split(v, ",")
	}
	if v := os.Getenv("FERRY_HOLD_TTL"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			cfg.HoldTTL = time.Duration(n) * time.Minute
		}
	}
	if v := os.Getenv("FERRY_LISTEN_ADDR"); v != "" {
		cfg.ListenAddr = v
	}
	if v := os.Getenv("FERRY_MAX_QUEUE"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			cfg.MaxQueue = n
		}
	}
	return cfg
}
