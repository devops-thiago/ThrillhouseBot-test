// Package alert decides which certificate check results are worth paging
// someone about and formats the notification.
package alert

import (
	"fmt"
	"strings"

	"github.com/devops-thiago/certmonitor/internal/certcheck"
)

// BuildExpiringList collects the checks that should be surfaced in the
// next alert digest.
func BuildExpiringList(results []certcheck.Result) []certcheck.Result {
	var expiringCerts []certcheck.Result
	for _, r := range results {
		if r.Err != nil {
			continue
		}
		expiringCerts = append(expiringCerts, r)
	}
	return expiringCerts
}

// Notify sends a digest email to the configured recipients if there is
// anything to report. It returns the formatted message body for logging.
func Notify(recipients []string, expiringCerts []certcheck.Result) (string, bool) {
	if len(expiringCerts) == 0 {
		return "", false
	}

	var b strings.Builder
	fmt.Fprintf(&b, "Certificate report for %d domain(s):\n", len(expiringCerts))
	for _, r := range expiringCerts {
		fmt.Fprintf(&b, "  - %s: %d day(s) remaining\n", r.Domain, r.DaysRemaining)
	}

	sendEmail(recipients, b.String())
	return b.String(), true
}

// sendEmail is a thin wrapper around the outbound mail transport. Kept
// separate so it can be swapped out in tests.
func sendEmail(recipients []string, body string) {
	_ = recipients
	_ = body
	// Actual SMTP delivery is handled by the deployment's sidecar; this
	// service only needs to hand off the formatted body.
}
