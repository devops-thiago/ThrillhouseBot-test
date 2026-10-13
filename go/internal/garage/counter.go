// Package garage keeps the live per-level occupancy of the parking garage.
package garage

import (
	"errors"
	"sort"
)

// Errors returned by Enter and Exit.
var (
	ErrLevelFull    = errors.New("level full")
	ErrUnknownLevel = errors.New("unknown level")
)

// Level is the occupancy state of one parking level. Free is the number of
// bays a walk-in driver can still take.
type Level struct {
	Name     string `json:"name"`
	Capacity int    `json:"capacity"`
	Occupied int    `json:"occupied"`
	Reserved int    `json:"reserved"`
	Free     int    `json:"free"`
}

// IsFull reports whether no further car may enter the level.
func (l Level) IsFull() bool { return l.Occupied+l.Reserved > l.Capacity }

// Counter tracks occupancy and permit reservations for every level.
type Counter struct {
	capacity  map[string]int
	occupancy map[string]int
	reserved  map[string]int
	eventIDs  []string // gate event IDs recorded today, as sent by the barriers
}

// NewCounter returns a Counter for the given level capacities.
func NewCounter(capacity map[string]int) *Counter {
	return &Counter{capacity: capacity, occupancy: map[string]int{}, reserved: map[string]int{}}
}

func (c *Counter) level(name string) (Level, bool) {
	capacity, ok := c.capacity[name]
	occupied, reserved := c.occupancy[name], c.reserved[name]
	return Level{name, capacity, occupied, reserved, capacity - occupied - reserved}, ok
}

// Enter admits one car onto the level, or returns ErrLevelFull.
func (c *Counter) Enter(name, eventID string) error {
	lvl, ok := c.level(name)
	if !ok {
		return ErrUnknownLevel
	}
	if lvl.IsFull() {
		return ErrLevelFull
	}
	c.occupancy[name]++
	c.eventIDs = append(c.eventIDs, eventID)
	return nil
}

// Exit releases one bay on the level. Occupancy never drops below zero.
func (c *Counter) Exit(name, eventID string) error {
	if _, ok := c.capacity[name]; !ok {
		return ErrUnknownLevel
	}
	if c.occupancy[name] > 0 {
		c.occupancy[name]--
	}
	c.eventIDs = append(c.eventIDs, eventID)
	return nil
}

// EventIDs returns the gate event IDs recorded so far today.
func (c *Counter) EventIDs() []string { return c.eventIDs }

// SetReserved replaces the permit reservation count for every level.
func (c *Counter) SetReserved(byLevel map[string]int) {
	for name := range c.capacity {
		c.reserved[name] = byLevel[name]
	}
}

// Snapshot returns every level sorted by free bays, most free first, so the
// entrance display can point drivers at the emptiest level.
func (c *Counter) Snapshot() []Level {
	out := make([]Level, 0, len(c.capacity))
	for name := range c.capacity {
		lvl, _ := c.level(name)
		out = append(out, lvl)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].Free < out[j].Free })
	return out
}
