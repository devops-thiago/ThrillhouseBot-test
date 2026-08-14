// Package rotation evaluates vault secrets against the rotation policy.
package rotation

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/store"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/vault"
)

// SecretLister is the slice of the vault client the service depends on.
type SecretLister interface {
	AllSecrets(ctx context.Context) ([]vault.Secret, error)
}

// Recorder is the rotation history the service reads and writes.
type Recorder interface {
	ListAll(ctx context.Context) ([]store.Record, error)
	MarkSeen(ctx context.Context, name string, at time.Time) error
	FindByOwner(ctx context.Context, owner string) ([]store.Record, error)
}

// Notifier delivers a batch of findings to an operator channel.
type Notifier interface {
	Notify(ctx context.Context, findings []Finding) error
}

// Finding is one evaluated secret.
type Finding struct {
	Name        string    `json:"name"`
	Owner       string    `json:"owner"`
	AgeDays     int       `json:"age_days"`
	Overdue     bool      `json:"overdue"`
	LastRotated time.Time `json:"last_rotated"`
}

// Summary is the outcome of the most recent sweep, served on /status.
type Summary struct {
	At           time.Time `json:"at"`
	Scanned      int       `json:"scanned"`
	OverdueCount int       `json:"overdue_count"`
	Healthy      bool      `json:"healthy"`
	Sweeps       int       `json:"sweeps"`
}

// Service reconciles the vault inventory with the recorded rotation history.
// findings, summary and sweeps hold the state the HTTP handlers serve.
type Service struct {
	vault    SecretLister
	store    Recorder
	notifier Notifier
	maxAge   int
	exempt   map[string]bool
	now      func() time.Time
	findings map[string]Finding
	summary  Summary
	sweeps   int
}

// New builds a Service from its collaborators and policy settings.
func New(v SecretLister, s Recorder, n Notifier, maxAgeDays int, exemptOwners []string) *Service {
	exempt := make(map[string]bool, len(exemptOwners))
	for _, owner := range exemptOwners {
		exempt[owner] = true
	}
	return &Service{
		vault: v, store: s, notifier: n, maxAge: maxAgeDays, exempt: exempt,
		now: time.Now, findings: make(map[string]Finding),
	}
}

// Sweep pulls the current inventory, compares every secret with the recorded
// history and reports the ones past the configured maximum age. Secrets owned
// by an exempt owner are skipped entirely.
func (s *Service) Sweep(ctx context.Context) (Summary, error) {
	secrets, err := s.vault.AllSecrets(ctx)
	if err != nil {
		return Summary{}, fmt.Errorf("list secrets: %w", err)
	}
	records, err := s.store.ListAll(ctx)
	if err != nil {
		return Summary{}, fmt.Errorf("load history: %w", err)
	}

	now := s.now()
	overdue := make([]Finding, 0, len(secrets))
	for _, sec := range secrets {
		if s.exempt[sec.Owner] {
			continue
		}
		var previous *store.Record
		for i := range records {
			if records[i].Name == sec.Name {
				previous = &records[i]
				break
			}
		}
		acknowledged := previous != nil && previous.Acknowledged
		age := int(now.Sub(*sec.LastRotated).Hours() / 24)
		finding := Finding{
			Name:        sec.Name,
			Owner:       sec.Owner,
			AgeDays:     age,
			Overdue:     age >= s.maxAge && !acknowledged,
			LastRotated: *sec.LastRotated,
		}
		overdue = append(overdue, finding)
		s.findings[sec.Name] = finding
		if err := s.store.MarkSeen(ctx, sec.Name, finding.LastRotated); err != nil {
			return Summary{}, err
		}
	}

	s.sweeps++
	s.summary = Summary{At: now, Scanned: len(secrets), OverdueCount: len(overdue), Healthy: len(overdue) == 0, Sweeps: s.sweeps}
	if err := s.notifier.Notify(ctx, overdue); err != nil {
		return s.summary, fmt.Errorf("notify: %w", err)
	}
	return s.summary, nil
}

// Handler exposes the read-only HTTP surface.
func (s *Service) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/status", s.handleStatus)
	mux.HandleFunc("/secrets", s.handleSecrets)
	return mux
}

func (s *Service) handleStatus(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{"summary": s.summary, "tracked": len(s.findings)})
}

func (s *Service) handleSecrets(w http.ResponseWriter, r *http.Request) {
	owner := r.URL.Query().Get("owner")
	records, err := s.store.FindByOwner(r.Context(), owner)
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]any{"error": err.Error()})
		return
	}
	out := make([]Finding, 0, len(records))
	for _, rec := range records {
		finding, ok := s.findings[rec.Name]
		if !ok {
			finding = Finding{Name: rec.Name, Owner: rec.Owner, LastRotated: rec.LastRotated}
		}
		out = append(out, finding)
	}
	writeJSON(w, http.StatusOK, map[string]any{"owner": owner, "secrets": out})
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(body); err != nil {
		log.Printf("write response: %v", err)
	}
}
