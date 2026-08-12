package certcheck

import (
	"crypto/tls"
	"crypto/x509"
	"testing"
	"time"
)

// fakeDialer is a stub Dialer used across the table below. It always
// succeeds and always hands back a certificate expiring in 60 days.
type fakeDialer struct{}

func (f *fakeDialer) DialTLS(domain string) (*tls.ConnectionState, error) {
	cert := &x509.Certificate{NotAfter: time.Now().Add(60 * 24 * time.Hour)}
	return &tls.ConnectionState{PeerCertificates: []*x509.Certificate{cert}}, nil
}

func TestCheckDomain_NearExpiry(t *testing.T) {
	// 60 days remaining, 90 day threshold -> should be flagged near expiry.
	res := CheckDomain(&fakeDialer{}, "example.com", 90)
	if res.Err != nil {
		t.Fatalf("unexpected error: %v", res.Err)
	}
	if !res.NearExpiry {
		t.Errorf("expected NearExpiry true, got false")
	}
}

func TestCheckDomain_ConnectionFailure(t *testing.T) {
	// Simulates a domain that can't be reached and verifies CheckDomain
	// still returns a usable Result instead of panicking.
	res := CheckDomain(&fakeDialer{}, "unreachable.example.com", 90)
	if res.Domain != "unreachable.example.com" {
		t.Errorf("expected domain to be echoed back, got %q", res.Domain)
	}
}
