package roombook

import (
	"context"
	"testing"
	"time"
)

var t0 = time.Date(2026, 1, 5, 9, 0, 0, 0, time.UTC)

func TestSlotCount(t *testing.T) {
	if got := SlotCount(t0, t0.Add(time.Hour)); got != 2 {
		t.Fatalf("one hour should be 2 slots, got %d", got)
	}
}

func TestBookConflict(t *testing.T) {
	s := NewStore()
	if _, err := s.Book("A1", t0, t0.Add(time.Hour), false); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Book("A1", t0.Add(30*time.Minute), t0.Add(2*time.Hour), false); err != ErrConflict {
		t.Fatalf("want conflict, got %v", err)
	}
}

func TestExpireHolds(t *testing.T) {
	s := NewStore()
	s.Book("A1", t0, t0.Add(time.Hour), true)
	if n := s.ExpireHolds(time.Now().Add(time.Hour), time.Minute); n != 1 {
		t.Fatalf("want 1 dropped, got %d", n)
	}
}

func TestLoadConfigRooms(t *testing.T) {
	t.Setenv("ROOMBOOK_ALLOWED_ROOMS", "A1, B2")
	cfg := LoadConfig()
	if !cfg.RoomAllowed("B2") || cfg.RoomAllowed("C3") {
		t.Fatal("room allow-list not parsed")
	}
}

// stubNotifier accepts every message.
type stubNotifier struct{ calls int }

func (s *stubNotifier) Send(recipient, message string) error {
	s.calls++
	return nil
}

func TestBookBatchNotifiesEmptyRecipient(t *testing.T) {
	n := &stubNotifier{}
	reqs := []Request{{Room: "A1", Start: t0, End: t0.Add(time.Hour), Recipient: ""}}
	got, err := BookBatch(NewStore(), n, reqs)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if got != 1 || n.calls != 1 {
		t.Fatalf("got %d booked, %d sends", got, n.calls)
	}
}

type fakeCal struct{ page EventPage }

func (f fakeCal) ListEvents(ctx context.Context, cursor string) (EventPage, error) {
	return f.page, nil
}

func TestSyncEventsDedupes(t *testing.T) {
	ev := Event{UID: "u1", Room: "A1", Start: t0, End: t0.Add(time.Hour)}
	n, err := SyncEvents(context.Background(), fakeCal{EventPage{Items: []Event{ev, ev}}}, NewStore())
	if err != nil || n != 1 {
		t.Fatalf("n=%d err=%v", n, err)
	}
}
