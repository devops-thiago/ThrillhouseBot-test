// Package expiry reconciles the service inventory with the certificates the
// internal CA has issued and reports what is close to running out.
package expiry

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/inventory"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/issuer"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/store"
)

// Severities a scanned endpoint can carry.
const (
	SeverityOK       = "ok"
	SeverityWarning  = "warning"
	SeverityCritical = "critical"
	SeverityExpired  = "expired"
	SeverityMissing  = "missing"
)

// EndpointSource is the slice of the inventory client the service depends on.
type EndpointSource interface {
	Endpoints(ctx context.Context) ([]inventory.Endpoint, error)
}

// CertificateSource lists the certificates the CA has issued.
type CertificateSource interface {
	Issued(ctx context.Context) ([]issuer.Certificate, error)
}

// StateStore is the persistence the service reads waivers from and writes scan
// state to.
type StateStore interface {
	ActiveWaivers(ctx context.Context, asOf time.Time) ([]store.Waiver, error)
	SaveState(ctx context.Context, state store.State) error
	StatesByOwner(ctx context.Context, owner string) ([]store.State, error)
}

// DigestSender delivers one owner's findings to the notification channel.
type DigestSender interface {
	SendDigest(ctx context.Context, owner string, findings []Finding) error
}

// Finding is the outcome for a single inventory endpoint.
type Finding struct {
	Host     string    `json:"host"`
	Owner    string    `json:"owner"`
	Serial   string    `json:"serial,omitempty"`
	NotAfter time.Time `json:"not_after,omitempty"`
	DaysLeft int       `json:"days_left"`
	Severity string    `json:"severity"`
}

// Summary describes the most recent scan and is what /report serves.
type Summary struct {
	At           time.Time `json:"at"`
	Endpoints    int       `json:"endpoints"`
	Certificates int       `json:"certificates"`
	Waived       int       `json:"waived"`
	Reported     int       `json:"reported"`
	Missing      int       `json:"missing"`
	Expired      int       `json:"expired"`
	Owners       int       `json:"owners"`
	Scans        int       `json:"scans"`
}

// Service holds the scan policy and the result of the last scan.
type Service struct {
	endpoints EndpointSource
	certs     CertificateSource
	store     StateStore
	digest    DigestSender
	warnDays  int
	critDays  int
	skip      map[string]bool
	now       func() time.Time

	mu       sync.RWMutex
	summary  Summary
	findings map[string]Finding
	scans    int
}

// New builds a Service from its collaborators and the expiry thresholds.
func New(e EndpointSource, c CertificateSource, s StateStore, d DigestSender,
	warnDays, criticalDays int, skipOwners []string) *Service {
	skip := make(map[string]bool, len(skipOwners))
	for _, owner := range skipOwners {
		skip[strings.ToLower(owner)] = true
	}
	return &Service{
		endpoints: e, certs: c, store: s, digest: d,
		warnDays: warnDays, critDays: criticalDays, skip: skip,
		now: time.Now, findings: make(map[string]Finding),
	}
}

// Scan pulls the inventory and the issued certificates, classifies every
// endpoint and sends one digest per owner that has something to act on.
func (s *Service) Scan(ctx context.Context) (Summary, error) {
	endpoints, err := s.endpoints.Endpoints(ctx)
	if err != nil {
		return Summary{}, fmt.Errorf("list endpoints: %w", err)
	}
	certificates, err := s.certs.Issued(ctx)
	if err != nil {
		return Summary{}, fmt.Errorf("list certificates: %w", err)
	}

	now := s.now()
	waivers, err := s.store.ActiveWaivers(ctx, now)
	if err != nil {
		return Summary{}, fmt.Errorf("load waivers: %w", err)
	}

	waived := make(map[string]bool, len(waivers))
	for _, w := range waivers {
		waived[normaliseHost(w.Host)] = true
	}
	index := indexCertificates(certificates)

	summary := Summary{At: now, Endpoints: len(endpoints), Certificates: len(certificates)}
	findings := make(map[string]Finding, len(endpoints))
	byOwner := make(map[string][]Finding)

	for _, ep := range endpoints {
		host := normaliseHost(ep.Host)
		if host == "" || s.skip[strings.ToLower(ep.Owner)] {
			continue
		}
		if waived[host] {
			summary.Waived++
			continue
		}

		finding := s.classify(ep, index[host], now)
		findings[host] = finding
		if finding.Severity == SeverityOK {
			continue
		}

		state := store.State{
			Host: finding.Host, Owner: finding.Owner, Serial: finding.Serial,
			NotAfter: finding.NotAfter, Severity: finding.Severity, CheckedAt: now,
		}
		if err := s.store.SaveState(ctx, state); err != nil {
			return Summary{}, err
		}
		byOwner[finding.Owner] = append(byOwner[finding.Owner], finding)
		summary.Reported++
		switch finding.Severity {
		case SeverityMissing:
			summary.Missing++
		case SeverityExpired:
			summary.Expired++
		}
	}

	summary.Owners = len(byOwner)
	s.mu.Lock()
	s.scans++
	summary.Scans = s.scans
	s.summary = summary
	s.findings = findings
	s.mu.Unlock()

	return summary, s.deliver(ctx, byOwner)
}

// classify turns one endpoint and the certificate covering it into a Finding.
// A zero certificate means the CA has nothing issued for the host at all.
func (s *Service) classify(ep inventory.Endpoint, cert issuer.Certificate, now time.Time) Finding {
	finding := Finding{Host: normaliseHost(ep.Host), Owner: ep.Owner}
	if finding.Owner == "" {
		finding.Owner = "unassigned"
	}
	if cert.Serial == "" {
		finding.Severity = SeverityMissing
		return finding
	}

	finding.Serial = cert.Serial
	finding.NotAfter = cert.NotAfter
	finding.DaysLeft = int(cert.NotAfter.Sub(now) / (24 * time.Hour))
	switch {
	case !cert.NotAfter.After(now):
		finding.Severity = SeverityExpired
	case finding.DaysLeft <= s.critDays:
		finding.Severity = SeverityCritical
	case finding.DaysLeft <= s.warnDays:
		finding.Severity = SeverityWarning
	default:
		finding.Severity = SeverityOK
	}
	return finding
}

// deliver sends one digest per owner, soonest expiry first inside each digest.
// A failing owner does not stop the others; the errors are joined instead.
func (s *Service) deliver(ctx context.Context, byOwner map[string][]Finding) error {
	owners := make([]string, 0, len(byOwner))
	for owner := range byOwner {
		owners = append(owners, owner)
	}
	sort.Strings(owners)

	var errs []error
	for _, owner := range owners {
		findings := byOwner[owner]
		sort.Slice(findings, func(i, j int) bool { return findings[i].DaysLeft < findings[j].DaysLeft })
		if err := s.digest.SendDigest(ctx, owner, findings); err != nil {
			errs = append(errs, fmt.Errorf("digest for %s: %w", owner, err))
		}
	}
	return errors.Join(errs...)
}

// indexCertificates maps every host a certificate covers to that certificate.
// Superseded certificates stay in the CA listing until they expire, so the one
// with the latest expiry wins; revoked certificates are ignored outright.
func indexCertificates(certificates []issuer.Certificate) map[string]issuer.Certificate {
	index := make(map[string]issuer.Certificate, len(certificates))
	for _, cert := range certificates {
		if cert.Revoked {
			continue
		}
		for _, name := range append([]string{cert.Subject}, cert.SANs...) {
			host := normaliseHost(name)
			if host == "" {
				continue
			}
			if current, ok := index[host]; ok && !cert.NotAfter.After(current.NotAfter) {
				continue
			}
			index[host] = cert
		}
	}
	return index
}

// normaliseHost lowercases a host name and drops the trailing dot the CA
// sometimes keeps on fully qualified names.
func normaliseHost(host string) string {
	return strings.TrimSuffix(strings.ToLower(strings.TrimSpace(host)), ".")
}

// Handler exposes the read-only HTTP surface.
func (s *Service) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", s.handleHealth)
	mux.HandleFunc("/report", s.handleReport)
	mux.HandleFunc("/findings", s.handleFindings)
	return mux
}

func (s *Service) handleHealth(w http.ResponseWriter, _ *http.Request) {
	s.mu.RLock()
	scans := s.scans
	s.mu.RUnlock()
	writeJSON(w, http.StatusOK, map[string]any{"status": "ok", "scans": scans})
}

func (s *Service) handleReport(w http.ResponseWriter, _ *http.Request) {
	s.mu.RLock()
	summary, tracked := s.summary, len(s.findings)
	s.mu.RUnlock()
	writeJSON(w, http.StatusOK, map[string]any{"summary": summary, "tracked": tracked})
}

// handleFindings answers /findings?owner=… from the recorded state, falling
// back to the store for hosts the running process has not scanned yet.
func (s *Service) handleFindings(w http.ResponseWriter, r *http.Request) {
	owner := strings.TrimSpace(r.URL.Query().Get("owner"))
	if owner == "" {
		writeJSON(w, http.StatusBadRequest, map[string]any{"error": "owner is required"})
		return
	}
	states, err := s.store.StatesByOwner(r.Context(), owner)
	if err != nil {
		log.Printf("findings for %s: %v", owner, err)
		writeJSON(w, http.StatusInternalServerError, map[string]any{"error": "lookup failed"})
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]Finding, 0, len(states))
	for _, state := range states {
		if finding, ok := s.findings[state.Host]; ok {
			out = append(out, finding)
			continue
		}
		out = append(out, Finding{
			Host: state.Host, Owner: state.Owner, Serial: state.Serial,
			NotAfter: state.NotAfter, Severity: state.Severity,
		})
	}
	writeJSON(w, http.StatusOK, map[string]any{"owner": owner, "findings": out})
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(body); err != nil {
		log.Printf("write response: %v", err)
	}
}
