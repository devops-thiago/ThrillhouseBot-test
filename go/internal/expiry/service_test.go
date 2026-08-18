package expiry

import (
	"context"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/inventory"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/issuer"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/store"
)

var scanTime = time.Date(2026, time.June, 1, 12, 0, 0, 0, time.UTC)

type stubInventory struct {
	endpoints []inventory.Endpoint
}

func (s *stubInventory) Endpoints(context.Context) ([]inventory.Endpoint, error) {
	return s.endpoints, nil
}

type stubIssuer struct {
	certificates []issuer.Certificate
}

func (s *stubIssuer) Issued(context.Context) ([]issuer.Certificate, error) {
	return s.certificates, nil
}

type stubStore struct {
	waivers []store.Waiver
	saved   []store.State
}

func (s *stubStore) ActiveWaivers(_ context.Context, asOf time.Time) ([]store.Waiver, error) {
	var out []store.Waiver
	for _, w := range s.waivers {
		if w.Until.After(asOf) {
			out = append(out, w)
		}
	}
	return out, nil
}

func (s *stubStore) SaveState(_ context.Context, state store.State) error {
	s.saved = append(s.saved, state)
	return nil
}

func (s *stubStore) StatesByOwner(_ context.Context, owner string) ([]store.State, error) {
	var out []store.State
	for _, state := range s.saved {
		if state.Owner == owner {
			out = append(out, state)
		}
	}
	return out, nil
}

type stubDigest struct {
	sent map[string][]Finding
}

func (s *stubDigest) SendDigest(_ context.Context, owner string, findings []Finding) error {
	if s.sent == nil {
		s.sent = map[string][]Finding{}
	}
	s.sent[owner] = findings
	return nil
}

func endpoint(host, owner string) inventory.Endpoint {
	return inventory.Endpoint{Host: host, Port: 443, Owner: owner, Environment: "production"}
}

func certificate(host string, expiresInDays int) issuer.Certificate {
	return issuer.Certificate{
		Serial:   fmt.Sprintf("serial-%s", host),
		Subject:  host,
		NotAfter: scanTime.AddDate(0, 0, expiresInDays),
	}
}

func newService(inv *stubInventory, iss *stubIssuer, st *stubStore, dg *stubDigest, skip ...string) *Service {
	svc := New(inv, iss, st, dg, 30, 7, skip)
	svc.now = func() time.Time { return scanTime }
	return svc
}

func TestScanClassifiesByRemainingLifetime(t *testing.T) {
	inv := &stubInventory{endpoints: []inventory.Endpoint{
		endpoint("api.example.com", "platform"),
		endpoint("checkout.example.com", "payments"),
		endpoint("legacy.example.com", "payments"),
		endpoint("shop.example.com", "storefront"),
	}}
	iss := &stubIssuer{certificates: []issuer.Certificate{
		certificate("api.example.com", 200),
		certificate("checkout.example.com", 3),
		certificate("legacy.example.com", -5),
	}}
	digest := &stubDigest{}

	summary, err := newService(inv, iss, &stubStore{}, digest).Scan(context.Background())
	if err != nil {
		t.Fatalf("scan: %v", err)
	}

	if summary.Endpoints != 4 || summary.Reported != 3 {
		t.Fatalf("summary = %+v, want 4 endpoints and 3 reported", summary)
	}
	if summary.Missing != 1 || summary.Expired != 1 {
		t.Fatalf("summary = %+v, want one missing and one expired", summary)
	}
	if got := len(digest.sent); got != 2 {
		t.Fatalf("digests sent to %d owners, want 2", got)
	}
	payments := digest.sent["payments"]
	if len(payments) != 2 {
		t.Fatalf("payments digest has %d findings, want 2", len(payments))
	}
	if payments[0].Severity != SeverityExpired || payments[1].Severity != SeverityCritical {
		t.Fatalf("payments digest = %+v, want expired before critical", payments)
	}
	if payments[1].DaysLeft != 3 {
		t.Fatalf("days left = %d, want 3", payments[1].DaysLeft)
	}
}

func TestScanPrefersTheLatestCertificateForAHost(t *testing.T) {
	inv := &stubInventory{endpoints: []inventory.Endpoint{endpoint("API.example.com.", "platform")}}
	renewed := certificate("api.example.com", 90)
	renewed.Serial = "serial-renewed"
	superseded := certificate("api.example.com", 2)
	revoked := issuer.Certificate{
		Serial: "serial-revoked", Subject: "other.example.com",
		SANs: []string{"api.example.com"}, NotAfter: scanTime.AddDate(1, 0, 0), Revoked: true,
	}
	iss := &stubIssuer{certificates: []issuer.Certificate{superseded, renewed, revoked}}
	digest := &stubDigest{}

	summary, err := newService(inv, iss, &stubStore{}, digest).Scan(context.Background())
	if err != nil {
		t.Fatalf("scan: %v", err)
	}
	if summary.Reported != 0 {
		t.Fatalf("reported %d findings, want none", summary.Reported)
	}
	if len(digest.sent) != 0 {
		t.Fatalf("sent %d digests for a healthy host", len(digest.sent))
	}
}

func TestScanSkipsWaivedHostsAndSkippedOwners(t *testing.T) {
	inv := &stubInventory{endpoints: []inventory.Endpoint{
		endpoint("waived.example.com", "platform"),
		endpoint("labs.example.com", "Research"),
		endpoint("api.example.com", "platform"),
	}}
	iss := &stubIssuer{}
	st := &stubStore{waivers: []store.Waiver{
		{Host: "waived.example.com", Until: scanTime.AddDate(0, 0, 10), Reason: "cdn migration"},
		{Host: "api.example.com", Until: scanTime.AddDate(0, 0, -1), Reason: "expired waiver"},
	}}
	digest := &stubDigest{}

	summary, err := newService(inv, iss, st, digest, "research").Scan(context.Background())
	if err != nil {
		t.Fatalf("scan: %v", err)
	}

	if summary.Waived != 1 || summary.Reported != 1 {
		t.Fatalf("summary = %+v, want one waived and one reported", summary)
	}
	if len(st.saved) != 1 || st.saved[0].Host != "api.example.com" {
		t.Fatalf("saved state = %+v, want only api.example.com", st.saved)
	}
	if st.saved[0].Severity != SeverityMissing {
		t.Fatalf("severity = %q, want %q", st.saved[0].Severity, SeverityMissing)
	}
}

func TestFindingsEndpointServesTheOwnersHosts(t *testing.T) {
	inv := &stubInventory{endpoints: []inventory.Endpoint{endpoint("api.example.com", "platform")}}
	iss := &stubIssuer{certificates: []issuer.Certificate{certificate("api.example.com", 5)}}
	svc := newService(inv, iss, &stubStore{}, &stubDigest{})
	if _, err := svc.Scan(context.Background()); err != nil {
		t.Fatalf("scan: %v", err)
	}

	server := httptest.NewServer(svc.Handler())
	defer server.Close()

	resp, err := http.Get(server.URL + "/findings?owner=platform")
	if err != nil {
		t.Fatalf("get findings: %v", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("status = %d, want 200", resp.StatusCode)
	}

	missing, err := http.Get(server.URL + "/findings")
	if err != nil {
		t.Fatalf("get findings without owner: %v", err)
	}
	defer missing.Body.Close()
	if missing.StatusCode != http.StatusBadRequest {
		t.Fatalf("status = %d, want 400", missing.StatusCode)
	}
}
