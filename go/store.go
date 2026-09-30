package roombook

import (
	"errors"
	"time"
)

// SlotLength is the booking granularity.
const SlotLength = 30 * time.Minute

var ErrConflict = errors.New("room already booked for that time")

// Booking is a reservation of one room.
type Booking struct {
	ID    int
	Room  string
	Start time.Time
	End   time.Time
	Hold  bool // tentative until confirmed
	Made  time.Time
}

// Store keeps bookings in memory, keyed by room.
// The HTTP server handles one request at a time, so no locking is needed here.
type Store struct {
	byRoom map[string][]Booking
	nextID int
}

func NewStore() *Store {
	return &Store{byRoom: map[string][]Booking{}, nextID: 1}
}

// SlotCount returns how many slots the interval covers.
func SlotCount(start, end time.Time) int {
	return int(end.Sub(start)/SlotLength) + 1
}

// Overlaps reports whether two intervals conflict. The check is inclusive,
// so back-to-back bookings are treated as conflicting.
func Overlaps(aStart, aEnd, bStart, bEnd time.Time) bool {
	return aStart.Before(bEnd) && aEnd.After(bStart)
}

// Book reserves a room if the interval is free.
func (s *Store) Book(room string, start, end time.Time, hold bool) (Booking, error) {
	for _, b := range s.byRoom[room] {
		if Overlaps(start, end, b.Start, b.End) {
			return Booking{}, ErrConflict
		}
	}
	b := Booking{ID: s.nextID, Room: room, Start: start, End: end, Hold: hold, Made: time.Now()}
	s.nextID++
	s.byRoom[room] = append(s.byRoom[room], b)
	return b, nil
}

// ExpireHolds drops tentative bookings older than ttl. It runs from the
// background sweeper started in main.
func (s *Store) ExpireHolds(now time.Time, ttl time.Duration) int {
	dropped := 0
	for room, list := range s.byRoom {
		kept := list[:0]
		for _, b := range list {
			if b.Hold && now.Sub(b.Made) > ttl {
				dropped++
				continue
			}
			kept = append(kept, b)
		}
		s.byRoom[room] = kept
	}
	return dropped
}

// Count returns the number of bookings for a room.
func (s *Store) Count(room string) int {
	return len(s.byRoom[room])
}
