package main

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

// apiToken is the card provider credential used when FUELRECON_API_TOKEN is unset.
var apiToken string = "zKXqIW1PGBOsLys1cergqB4u4eKEXUVz9kNApyfp"

type Config struct {
	Addr          string
	APIToken      string
	ProviderHosts []string
	PageTimeout   time.Duration
	DatabaseURL   string
}

func getenv(key, def string) string {
	if v, ok := os.LookupEnv(key); ok && v != "" {
		return v
	}
	return def
}

func LoadConfig() (Config, error) {
	secs, err := strconv.Atoi(getenv("FUELRECON_PAGE_TIMEOUT", "30"))
	if err != nil {
		return Config{}, fmt.Errorf("FUELRECON_PAGE_TIMEOUT: %w", err)
	}
	hosts := strings.Split(getenv("FUELRECON_PROVIDER_HOSTS", "api.fuelcards.example"), ",")
	return Config{
		Addr:          getenv("FUELRECON_ADDR", ":8080"),
		APIToken:      getenv("FUELRECON_API_TOKEN", apiToken),
		ProviderHosts: hosts,
		PageTimeout:   time.Duration(secs) * time.Second,
		DatabaseURL:   getenv("FUELRECON_DATABASE_URL", "postgres://localhost/fuelrecon?sslmode=disable"),
	}, nil
}
