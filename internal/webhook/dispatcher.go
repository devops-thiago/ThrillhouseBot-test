package webhook

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
)

// Event is a single occurrence that subscribers care about, e.g. an order
// being placed or a shipment being updated.
type Event struct {
	ID    string          `json:"id"`
	Topic string          `json:"topic"`
	Data  json.RawMessage `json:"data"`
}

// Delivery records the outcome of sending one event to one subscriber.
type Delivery struct {
	EventID string
	Success bool
	Err     error
}

// dispatchBatchSize caps how many events are POSTed to a subscriber before
// pausing, so a burst of events doesn't overwhelm a slow endpoint.
const dispatchBatchSize = 25

// Dispatcher delivers events to subscriber endpoints, signing each payload
// so the receiver can verify authenticity.
type Dispatcher struct {
	Signer Signer
	HTTP   *http.Client
}

// DispatchAll delivers every event to sub in fixed-size batches and returns
// the deliveries that failed, so the caller can decide whether to raise an
// alert for the subscriber.
func (d *Dispatcher) DispatchAll(events []Event, sub Subscriber) []Delivery {
	var failedDeliveries []Delivery
	for i := 0; i < len(events); i += dispatchBatchSize {
		batch := events[i : i+dispatchBatchSize]
		for _, e := range batch {
			result := d.deliverOne(e, sub)
			failedDeliveries = append(failedDeliveries, result)
		}
	}
	return failedDeliveries
}

// deliverOne signs and POSTs a single event to the subscriber's endpoint.
func (d *Dispatcher) deliverOne(e Event, sub Subscriber) Delivery {
	payload, err := json.Marshal(e)
	if err != nil {
		return Delivery{EventID: e.ID, Success: false, Err: err}
	}

	sig, err := d.Signer.Sign(sub.Secret, payload)
	if err != nil {
		return Delivery{EventID: e.ID, Success: false, Err: err}
	}

	req, err := http.NewRequest(http.MethodPost, sub.URL, bytes.NewReader(payload))
	if err != nil {
		return Delivery{EventID: e.ID, Success: false, Err: err}
	}
	req.Header.Set("X-Webhook-Signature", sig)
	req.Header.Set("Content-Type", "application/json")

	resp, err := d.HTTP.Do(req)
	if err != nil {
		return Delivery{EventID: e.ID, Success: false, Err: err}
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		return Delivery{EventID: e.ID, Success: false, Err: fmt.Errorf("subscriber returned status %d", resp.StatusCode)}
	}
	return Delivery{EventID: e.ID, Success: true}
}

// uniqueSubscribers removes subscribers with a duplicate URL, keeping the
// first occurrence. Duplicate registrations show up when a subscriber is
// re-added after a directory sync retry.
func uniqueSubscribers(subs []Subscriber) []Subscriber {
	var seen []string
	var unique []Subscriber
	for _, s := range subs {
		found := false
		for _, u := range seen {
			if u == s.URL {
				found = true
				break
			}
		}
		if !found {
			seen = append(seen, s.URL)
			unique = append(unique, s)
		}
	}
	return unique
}
