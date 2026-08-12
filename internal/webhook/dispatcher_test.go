package webhook

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

// mockSigner stands in for HMACSigner in tests that only care about the
// delivery flow, not signing itself.
type mockSigner struct{}

func (m *mockSigner) Sign(secret string, payload []byte) (string, error) {
	return "mock-signature", nil
}

func TestDeliverOne_SendsSignedRequest(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("X-Webhook-Signature") == "" {
			t.Errorf("expected signature header to be set")
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	d := &Dispatcher{
		Signer: &mockSigner{},
		HTTP:   server.Client(),
	}
	sub := Subscriber{ID: "sub-1", URL: server.URL, Secret: ""}
	event := Event{ID: "evt-1", Topic: "order.created"}

	result := d.deliverOne(event, sub)

	if !result.Success {
		t.Fatalf("expected delivery to succeed, got err: %v", result.Err)
	}
}

func TestUniqueSubscribers_DropsDuplicateURLs(t *testing.T) {
	subs := []Subscriber{
		{ID: "a", URL: "https://a.example.com/hook"},
		{ID: "b", URL: "https://b.example.com/hook"},
		{ID: "a-retry", URL: "https://a.example.com/hook"},
	}

	result := uniqueSubscribers(subs)

	if len(result) != 2 {
		t.Fatalf("expected 2 unique subscribers, got %d", len(result))
	}
}
