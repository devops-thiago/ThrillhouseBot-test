// Command scanwatch polls a container image scanner for vulnerability
// findings, aggregates them per image, and serves the results over HTTP.
package main

import (
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"scanwatch/internal/api"
	"scanwatch/internal/scanner"
	"scanwatch/internal/store"
)

// config holds the settings scanwatch reads from its environment.
type config struct {
	scannerURL      string
	scannerToken    string
	reportDir       string
	alertSeverities []string
	pollInterval    time.Duration
	listenAddr      string
}

func loadConfig() config {
	pollInterval := 10 * time.Minute
	if raw := os.Getenv("POLL_INTERVAL"); raw != "" {
		if d, err := time.ParseDuration(raw); err == nil {
			pollInterval = d
		} else {
			log.Printf("invalid POLL_INTERVAL %q, using default %s", raw, pollInterval)
		}
	}

	severities := []string{"critical"}
	if raw := os.Getenv("ALERT_SEVERITIES"); raw != "" {
		severities = strings.Split(raw, ",")
	}

	reportDir := os.Getenv("REPORT_DIR")
	if reportDir == "" {
		reportDir = "/var/lib/scanwatch/reports"
	}

	listenAddr := os.Getenv("LISTEN_ADDR")
	if listenAddr == "" {
		listenAddr = ":8080"
	}

	return config{
		scannerURL:      os.Getenv("SCANNER_API_URL"),
		scannerToken:    os.Getenv("SCANNER_API_TOKEN"),
		reportDir:       reportDir,
		alertSeverities: severities,
		pollInterval:    pollInterval,
		listenAddr:      listenAddr,
	}
}

func main() {
	cfg := loadConfig()
	if cfg.scannerURL == "" {
		log.Fatal("SCANNER_API_URL is required")
	}

	client := scanner.New(cfg.scannerURL, cfg.scannerToken)
	reportStore := store.New()

	srv := &api.Server{
		Scanner:   client,
		Store:     reportStore,
		ReportDir: cfg.reportDir,
	}

	log.Printf("scanwatch listening on %s (poll interval %s, alerting on %v)",
		cfg.listenAddr, cfg.pollInterval, cfg.alertSeverities)
	if err := http.ListenAndServe(cfg.listenAddr, srv.NewMux()); err != nil {
		log.Fatal(err)
	}
}
