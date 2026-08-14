// Command rotationd tracks secret rotation deadlines for a vault namespace.
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
	"github.com/devops-thiago/thrillhousebot-test/go/internal/notify"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/rotation"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/store"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/vault"
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

	svc := rotation.New(vault.New(cfg.VaultURL, cfg.VaultToken), store.New(db),
		notify.New(cfg.WebhookURL), cfg.MaxAgeDays, cfg.ExemptOwners)
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	srv := &http.Server{Addr: cfg.ListenAddr, Handler: svc.Handler(), ReadHeaderTimeout: 10 * time.Second}

	// The sweeper runs on its own goroutine while the HTTP server answers
	// /status and /secrets from the connection goroutines.
	go func() {
		ticker := time.NewTicker(cfg.SweepInterval)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				_ = srv.Close()
				return
			case <-ticker.C:
				if _, err := svc.Sweep(ctx); err != nil {
					log.Printf("sweep failed: %v", err)
				}
			}
		}
	}()

	log.Printf("rotationd listening on %s, sweeping every %s", cfg.ListenAddr, cfg.SweepInterval)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("serve: %v", err)
	}
}
