// Package digest delivers per-owner expiry digests to the notification relay.
package digest

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/expiry"
)

// maxEntries is how many findings a single digest carries. The relay renders
// one message per digest and truncates anything longer, so the overflow is
// reported as a count instead.
const maxEntries = 20

// Webhook posts digests to the notification relay.
type Webhook struct {
	url  string
	http *http.Client
}

// New builds a Webhook for the configured relay URL. An empty URL turns
// delivery into a no-op, which is what local runs use.
func New(url string) *Webhook {
	return &Webhook{url: url, http: &http.Client{Timeout: 10 * time.Second}}
}

// SendDigest posts one owner's findings. The relay is expected to route on the
// owner field, so the owner travels in the payload rather than in the path.
func (w *Webhook) SendDigest(ctx context.Context, owner string, findings []expiry.Finding) error {
	if w.url == "" || len(findings) == 0 {
		return nil
	}
	entries := findings
	if len(entries) > maxEntries {
		entries = entries[:maxEntries]
	}
	body, err := json.Marshal(map[string]any{
		"owner":     owner,
		"total":     len(findings),
		"truncated": len(findings) - len(entries),
		"findings":  entries,
	})
	if err != nil {
		return fmt.Errorf("encode digest: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, w.url, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := w.http.Do(req)
	if err != nil {
		return fmt.Errorf("post digest: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode >= http.StatusBadRequest {
		return fmt.Errorf("relay returned %d", resp.StatusCode)
	}
	return nil
}
