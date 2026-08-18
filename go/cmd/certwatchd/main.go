// Command certwatchd tracks how much life is left in the TLS certificates the
// internal CA has issued for the service inventory.
package main

import (
	"context"
	"database/sql"
	"errors"
	"log"
	"net/http"
	"os/signal"
	"syscall"
	"time"

	_ "github.com/lib/pq"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/config"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/digest"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/expiry"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/inventory"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/issuer"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/store"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("configuration: %v", err)
	}
	db, err := sql.Open("postgres", cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("open database: %v", err)
	}
	defer db.Close()

	svc := expiry.New(
		inventory.New(cfg.InventoryURL, cfg.APIToken),
		issuer.New(cfg.IssuerURL, cfg.APIToken),
		store.New(db),
		digest.New(cfg.DigestURL),
		cfg.WarnDays, cfg.CriticalDays, cfg.SkipOwners,
	)

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	srv := &http.Server{Addr: cfg.ListenAddr, Handler: svc.Handler(), ReadHeaderTimeout: 10 * time.Second}

	// The scanner runs on its own goroutine; the HTTP server answers /report
	// and /findings from the state the last scan left behind.
	go func() {
		ticker := time.NewTicker(cfg.ScanInterval)
		defer ticker.Stop()
		for {
			if summary, err := svc.Scan(ctx); err != nil {
				log.Printf("scan failed: %v", err)
			} else {
				log.Printf("scan %d: %d endpoints, %d reported, %d owners notified",
					summary.Scans, summary.Endpoints, summary.Reported, summary.Owners)
			}
			select {
			case <-ctx.Done():
				shutdownServer(srv)
				return
			case <-ticker.C:
			}
		}
	}()

	log.Printf("certwatchd listening on %s, scanning every %s (warn %dd, critical %dd)",
		cfg.ListenAddr, cfg.ScanInterval, cfg.WarnDays, cfg.CriticalDays)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("serve: %v", err)
	}
}

// shutdownServer stops the HTTP server, giving in-flight requests a few
// seconds to finish before the process exits.
func shutdownServer(srv *http.Server) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Printf("shutdown: %v", err)
	}
}
