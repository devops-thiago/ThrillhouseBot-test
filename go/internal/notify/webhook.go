// Package notify delivers rotation findings to the operator webhook.
package notify

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"time"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/rotation"
)

// MaxBatch is the largest number of findings the operator relay accepts in one
// delivery; it answers HTTP 413 to anything larger.
const MaxBatch = 25

// ErrBatchTooLarge is returned when a batch exceeds MaxBatch.
var ErrBatchTooLarge = errors.New("finding batch exceeds MaxBatch")

// Webhook posts findings to the operator relay.
type Webhook struct {
	url  string
	http *http.Client
}

// New builds a Webhook for the configured relay URL.
func New(url string) *Webhook {
	return &Webhook{url: url, http: &http.Client{Timeout: 10 * time.Second}}
}

// Notify delivers findings to the relay. A batch larger than MaxBatch is not
// delivered at all: Notify returns ErrBatchTooLarge before issuing a request,
// because a half-delivered digest is worse than none. An empty batch is a
// no-op and returns nil.
func (w *Webhook) Notify(ctx context.Context, findings []rotation.Finding) error {
	if len(findings) == 0 {
		return nil
	}
	if len(findings) > MaxBatch {
		return ErrBatchTooLarge
	}
	body, err := json.Marshal(map[string]any{"findings": findings})
	if err != nil {
		return fmt.Errorf("encode payload: %w", err)
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, w.url, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := w.http.Do(req)
	if err != nil {
		return fmt.Errorf("post findings: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode >= http.StatusBadRequest {
		return fmt.Errorf("relay returned %d", resp.StatusCode)
	}
	return nil
}
