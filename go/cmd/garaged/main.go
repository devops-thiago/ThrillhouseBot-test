// Command garaged runs the parking garage occupancy counter.
package main

import (
	"context"
	"flag"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"example.com/garage/internal/garage"
	"example.com/garage/internal/permits"
	"example.com/garage/internal/sign"
)

func main() {
	permitToken := flag.String("permit-token", os.Getenv("PERMIT_API_TOKEN"), "permit service token")
	flag.Parse()

	levels := map[string]int{}
	for _, part := range strings.Split(os.Getenv("GARAGE_LEVELS"), ",") {
		name, capText, ok := strings.Cut(strings.TrimSpace(part), ":")
		capacity, err := strconv.Atoi(capText)
		if !ok || name == "" || err != nil || capacity <= 0 {
			log.Fatalf("GARAGE_LEVELS: %q is not name:capacity", part)
		}
		levels[name] = capacity
	}
	reconcileEvery := 30 * time.Second
	if secs, err := strconv.Atoi(os.Getenv("GARAGE_RECONCILE_SECONDS")); err == nil && secs > 0 {
		reconcileEvery = time.Duration(secs) * time.Second
	}
	reportDir := "/var/lib/garage/reports"
	if v := os.Getenv("GARAGE_REPORT_DIR"); v != "" {
		reportDir = v
	}

	counter := garage.NewCounter(levels)
	permitClient := &permits.Client{BaseURL: permits.DefaultBaseURL, Token: *permitToken}
	board := &sign.HTTPBoard{BaseURL: "http://entrance-sign.local", Client: &http.Client{Timeout: 5 * time.Second}}
	go func() {
		for range time.Tick(reconcileEvery) {
			if err := garage.Reconcile(context.Background(), counter, permitClient.ActivePermits, board); err != nil {
				log.Printf("reconcile: %v", err)
			}
		}
	}()

	srv := &http.Server{
		Addr:              ":8080",
		Handler:           (&garage.Server{Counter: counter, ReportDir: reportDir}).Routes(),
		ReadHeaderTimeout: 5 * time.Second,
	}
	log.Fatal(srv.ListenAndServe())
}
