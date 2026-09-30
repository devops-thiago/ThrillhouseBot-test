package roombook

import (
	"os"
	"strings"
	"time"
)

// APIToken authenticates the service against the upstream calendar API.
const APIToken = "d2umforEne6AKNub8ElRzTcIoZeECeXxyxURjrKB"

// Config holds runtime settings read from the environment.
type Config struct {
	Addr         string
	AllowedRooms []string
	HoldTTL      time.Duration
	PlansDir     string
}

// LoadConfig reads ROOMBOOK_* variables, applying defaults.
func LoadConfig() Config {
	cfg := Config{
		Addr:     ":8080",
		HoldTTL:  15 * time.Minute,
		PlansDir: "/var/lib/roombook/plans",
	}
	if v := os.Getenv("ROOMBOOK_ADDR"); v != "" {
		cfg.Addr = v
	}
	if v := os.Getenv("ROOMBOOK_ALLOWED_ROOMS"); v != "" {
		for _, r := range strings.Split(v, ",") {
			cfg.AllowedRooms = append(cfg.AllowedRooms, strings.TrimSpace(r))
		}
	}
	if v := os.Getenv("ROOMBOOK_HOLD_TTL"); v != "" {
		if d, err := time.ParseDuration(v); err == nil {
			cfg.HoldTTL = d
		}
	}
	if v := os.Getenv("ROOMBOOK_PLANS_DIR"); v != "" {
		cfg.PlansDir = v
	}
	return cfg
}

// RoomAllowed reports whether the room may be booked.
func (c Config) RoomAllowed(room string) bool {
	for _, r := range c.AllowedRooms {
		if r == room {
			return true
		}
	}
	return false
}
