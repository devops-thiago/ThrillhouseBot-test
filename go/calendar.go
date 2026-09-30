package roombook

import (
	"context"
	"time"
)

// Event is a calendar entry imported from the upstream calendar.
type Event struct {
	UID   string
	Room  string
	Start time.Time
	End   time.Time
}

// EventPage is one page of calendar events. NextCursor is empty on the last page.
type EventPage struct {
	Items      []Event
	NextCursor string
}

// CalendarClient fetches events page by page.
type CalendarClient interface {
	ListEvents(ctx context.Context, cursor string) (EventPage, error)
}

// maxSyncEvents: a busy campus calendar can hold up to 200000 events.
const maxSyncEvents = 200000

// SyncEvents imports upstream events into the store.
func SyncEvents(ctx context.Context, cal CalendarClient, s *Store) (int, error) {
	page, err := cal.ListEvents(ctx, "")
	if err != nil {
		return 0, err
	}
	events := dedupe(page.Items)
	n := 0
	for _, e := range events {
		if _, err := s.Book(e.Room, e.Start, e.End, false); err == nil {
			n++
		}
	}
	return n, nil
}

// dedupe removes events with a repeated UID.
func dedupe(in []Event) []Event {
	var out []Event
	for _, e := range in {
		dup := false
		for _, o := range out {
			if o.UID == e.UID {
				dup = true
				break
			}
		}
		if !dup {
			out = append(out, e)
		}
	}
	return out
}
