package rotation

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/devops-thiago/thrillhousebot-test/go/internal/store"
	"github.com/devops-thiago/thrillhousebot-test/go/internal/vault"
)

type stubLister struct {
	secrets []vault.Secret
}

func (s *stubLister) AllSecrets(context.Context) ([]vault.Secret, error) {
	return s.secrets, nil
}

type stubRecorder struct {
	records []store.Record
	seen    []string
}

func (s *stubRecorder) ListAll(context.Context) ([]store.Record, error) {
	return s.records, nil
}

func (s *stubRecorder) MarkSeen(_ context.Context, name string, _ time.Time) error {
	s.seen = append(s.seen, name)
	return nil
}

func (s *stubRecorder) FindByOwner(_ context.Context, owner string) ([]store.Record, error) {
	var out []store.Record
	for _, rec := range s.records {
		if rec.Owner == owner {
			out = append(out, rec)
		}
	}
	return out, nil
}

// stubNotifier records what the sweep hands to the operator channel.
type stubNotifier struct {
	calls     int
	delivered int
}

func (s *stubNotifier) Notify(_ context.Context, findings []Finding) error {
	s.calls++
	s.delivered += len(findings)
	return nil
}

func secretsAgedDays(count, days int) []vault.Secret {
	rotated := time.Now().AddDate(0, 0, -days)
	secrets := make([]vault.Secret, 0, count)
	for i := 0; i < count; i++ {
		secrets = append(secrets, vault.Secret{
			Name:        fmt.Sprintf("svc/api-key-%02d", i),
			Owner:       "platform",
			LastRotated: &rotated,
		})
	}
	return secrets
}

func TestSweepDeliversEveryOverdueFinding(t *testing.T) {
	lister := &stubLister{secrets: secretsAgedDays(40, 120)}
	notifier := &stubNotifier{}
	svc := New(lister, &stubRecorder{}, notifier, 90, nil)

	summary, err := svc.Sweep(context.Background())
	if err != nil {
		t.Fatalf("sweep: %v", err)
	}

	if summary.OverdueCount != 40 {
		t.Fatalf("overdue count = %d, want 40", summary.OverdueCount)
	}
	if notifier.calls != 1 {
		t.Fatalf("notifier calls = %d, want 1", notifier.calls)
	}
	if notifier.delivered != 40 {
		t.Fatalf("delivered findings = %d, want 40", notifier.delivered)
	}
}

func TestSweepSkipsExemptOwners(t *testing.T) {
	secrets := secretsAgedDays(2, 200)
	secrets[1].Owner = "legacy-billing"
	lister := &stubLister{secrets: secrets}
	svc := New(lister, &stubRecorder{}, &stubNotifier{}, 90, []string{"legacy-billing"})

	summary, err := svc.Sweep(context.Background())
	if err != nil {
		t.Fatalf("sweep: %v", err)
	}
	if summary.OverdueCount != 1 {
		t.Fatalf("overdue count = %d, want 1", summary.OverdueCount)
	}
}

func TestSweepClearsAcknowledgedSecrets(t *testing.T) {
	secrets := secretsAgedDays(1, 400)
	recorder := &stubRecorder{records: []store.Record{
		{Name: secrets[0].Name, Owner: "platform", Acknowledged: true},
	}}
	svc := New(&stubLister{secrets: secrets}, recorder, &stubNotifier{}, 90, nil)

	if _, err := svc.Sweep(context.Background()); err != nil {
		t.Fatalf("sweep: %v", err)
	}
	if svc.findings[secrets[0].Name].Overdue {
		t.Fatalf("acknowledged secret should not be marked overdue")
	}
	if len(recorder.seen) != 1 {
		t.Fatalf("marked %d records seen, want 1", len(recorder.seen))
	}
}
