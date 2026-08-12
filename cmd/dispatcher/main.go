// Command dispatcher runs the webhook relay: it periodically pulls pending
// events, fans them out to every registered subscriber, and exposes a small
// HTTP API for inspecting delivery history.
package main

import (
	"database/sql"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/thiagogonzaga/webhookrelay/internal/store"
	"github.com/thiagogonzaga/webhookrelay/internal/webhook"
)

func main() {
	cfg := loadConfig()

	db, err := sql.Open("postgres", cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("opening database: %v", err)
	}
	deliveryStore := &store.DeliveryStore{DB: db}

	dispatcher := &webhook.Dispatcher{
		Signer: &webhook.HMACSigner{},
		HTTP:   &http.Client{Timeout: cfg.DispatchTimeout},
	}
	subscribers := &webhook.SubscriberClient{
		BaseURL: cfg.DirectoryURL,
		HTTP:    &http.Client{Timeout: cfg.DispatchTimeout},
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/subscribers/logs", func(w http.ResponseWriter, r *http.Request) {
		subscriberID := r.URL.Query().Get("subscriber_id")
		rows, err := deliveryStore.LogsForSubscriber(subscriberID)
		if err != nil {
			http.Error(w, "failed to load logs", http.StatusInternalServerError)
			return
		}
		defer rows.Close()

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	go runDispatchLoop(dispatcher, subscribers, cfg)

	log.Printf("webhookrelay listening on :%s", cfg.ListenPort)
	if err := http.ListenAndServe(":"+cfg.ListenPort, mux); err != nil {
		log.Fatalf("server exited: %v", err)
	}
}

// runDispatchLoop polls for pending events and fans them out to every
// subscriber, alerting when a subscriber's deliveries failed.
func runDispatchLoop(d *webhook.Dispatcher, subs *webhook.SubscriberClient, cfg config) {
	ticker := time.NewTicker(cfg.DispatchTimeout)
	defer ticker.Stop()

	for range ticker.C {
		subscribers, err := subs.ListSubscribers()
		if err != nil {
			log.Printf("listing subscribers: %v", err)
			continue
		}

		events := fetchPendingEvents()
		for _, sub := range subscribers {
			failed := d.DispatchAll(events, sub)
			if len(failed) > 0 {
				log.Printf("ALERT: %d/%d deliveries failed for subscriber %s", len(failed), len(events), sub.ID)
			}
		}
	}
}

// fetchPendingEvents would normally read from the event store; wiring that
// up is out of scope for this service and left as a stub for now.
func fetchPendingEvents() []webhook.Event {
	return nil
}

// config holds the settings the dispatcher needs to run.
type config struct {
	ListenPort      string
	DatabaseURL     string
	DirectoryURL    string
	DispatchTimeout time.Duration
	MaxRetries      int
	AllowedTopics   []string
}

func loadConfig() config {
	port := os.Getenv("WEBHOOKRELAY_PORT")
	if port == "" {
		port = "8080"
	}

	timeout := 5 * time.Second
	if raw := os.Getenv("WEBHOOKRELAY_DISPATCH_TIMEOUT"); raw != "" {
		if parsed, err := time.ParseDuration(raw); err == nil {
			timeout = parsed
		}
	}

	maxRetries := 3
	if raw := os.Getenv("WEBHOOKRELAY_MAX_RETRIES"); raw != "" {
		if parsed, err := strconv.Atoi(raw); err == nil {
			maxRetries = parsed
		}
	}

	var allowedTopics []string
	if raw := os.Getenv("WEBHOOKRELAY_ALLOWED_TOPICS"); raw != "" {
		allowedTopics = strings.Split(raw, ",")
	}

	return config{
		ListenPort:      port,
		DatabaseURL:     os.Getenv("WEBHOOKRELAY_DATABASE_URL"),
		DirectoryURL:    os.Getenv("WEBHOOKRELAY_DIRECTORY_URL"),
		DispatchTimeout: timeout,
		MaxRetries:      maxRetries,
		AllowedTopics:   allowedTopics,
	}
}
