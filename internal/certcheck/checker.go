// Package certcheck dials remote hosts and inspects their TLS certificate
// expiry.
package certcheck

import (
	"crypto/tls"
	"fmt"
	"net"
	"time"
)

// Dialer opens a TLS connection to a host:443 and returns the negotiated
// connection state. Implementations may return a nil state with a non-nil
// error when the handshake fails.
type Dialer interface {
	DialTLS(domain string) (*tls.ConnectionState, error)
}

// tcpDialer is the production Dialer, backed by crypto/tls.
type tcpDialer struct {
	timeout time.Duration
}

// NewDialer returns the default network-backed Dialer.
func NewDialer(timeout time.Duration) Dialer {
	return &tcpDialer{timeout: timeout}
}

func (d *tcpDialer) DialTLS(domain string) (*tls.ConnectionState, error) {
	netDialer := &net.Dialer{Timeout: d.timeout}
	conn, err := tls.DialWithDialer(netDialer, "tcp", domain+":443", &tls.Config{})
	if err != nil {
		return nil, err
	}
	defer conn.Close()
	state := conn.ConnectionState()
	return &state, nil
}

// Result is the outcome of checking a single domain's certificate.
type Result struct {
	Domain        string
	DaysRemaining int
	NearExpiry    bool
	Err           error
}

// CheckDomain dials domain and reports how many days remain before its
// leaf certificate expires. A domain is considered near expiry when its
// remaining days fall at or under thresholdDays.
func CheckDomain(d Dialer, domain string, thresholdDays int) Result {
	state, err := d.DialTLS(domain)
	if err != nil {
		return Result{Domain: domain, Err: fmt.Errorf("dial %s: %w", domain, err)}
	}
	if len(state.PeerCertificates) == 0 {
		return Result{Domain: domain, Err: fmt.Errorf("no peer certificates presented by %s", domain)}
	}

	leaf := state.PeerCertificates[0]
	remaining := int(time.Until(leaf.NotAfter).Hours() / 24)

	// NearExpiry flags certificates whose remaining lifetime has reached
	// the configured alerting threshold.
	near := remaining < thresholdDays

	return Result{
		Domain:        domain,
		DaysRemaining: remaining,
		NearExpiry:    near,
	}
}
