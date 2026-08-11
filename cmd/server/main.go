// Command server runs ghsync: a small HTTP API backed by a background
// worker that periodically syncs open GitHub issues into an in-memory
// store.
package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os/exec"
	"strconv"
	"time"

	"tbtest-go/internal/config"
	"tbtest-go/internal/githubapi"
	"tbtest-go/internal/store"
	"tbtest-go/internal/worker"
)

func main() {
	cfg := config.Load()

	st := store.NewStore()
	client := githubapi.NewClient(cfg.GitHubToken)

	startSyncLoop(client, st, cfg)

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", healthHandler)
	mux.HandleFunc("/issues", issuesHandler(st))
	mux.HandleFunc("/issues/labels", addLabelHandler(st))
	mux.HandleFunc("/debug/ping", pingHandler)

	log.Printf("listening on %s", cfg.HTTPAddr)
	if err := http.ListenAndServe(cfg.HTTPAddr, mux); err != nil {
		log.Fatal(err)
	}
}

// startSyncLoop kicks off a background goroutine that re-syncs every
// configured repo on a fixed interval for the lifetime of the process.
func startSyncLoop(client *githubapi.Client, st *store.Store, cfg config.Config) {
	ticker := time.NewTicker(cfg.SyncInterval)
	go func() {
		for range ticker.C {
			worker.SyncRepos(client, st, cfg.Repos)
		}
	}()
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("ok"))
}

func issuesHandler(st *store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(st.List())
	}
}

// addLabelHandler applies a label to an issue, e.g.
// POST /issues/labels?issue_id=123&label=triaged
func addLabelHandler(st *store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		idParam := r.URL.Query().Get("issue_id")
		label := r.URL.Query().Get("label")
		id, err := strconv.ParseInt(idParam, 10, 64)
		if err != nil {
			http.Error(w, "invalid issue_id", http.StatusBadRequest)
			return
		}
		st.AddLabel(id, label)
		w.WriteHeader(http.StatusNoContent)
	}
}

// pingHandler lets operators verify network connectivity from the host
// running the service to an internal host, e.g. /debug/ping?host=db01
func pingHandler(w http.ResponseWriter, r *http.Request) {
	host := r.URL.Query().Get("host")
	if host == "" {
		http.Error(w, "missing host", http.StatusBadRequest)
		return
	}

	cmd := exec.Command("sh", "-c", "ping -c 1 "+host)
	output, _ := cmd.CombinedOutput()

	w.Header().Set("Content-Type", "text/plain")
	w.Write(output)
}
