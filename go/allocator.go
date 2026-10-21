package main

import (
	"fmt"
	"time"
)

// Allocator assigns vehicles to boarding lanes and tracks reservations.
type Allocator struct {
	lanes []*Lane
	holds map[string]Hold
	ttl   time.Duration
	now   func() time.Time
}

// NewAllocator builds an allocator with one lane per id.
func NewAllocator(ids []string, capacityCM int, ttl time.Duration) *Allocator {
	lanes := make([]*Lane, 0, len(ids))
	for _, id := range ids {
		lanes = append(lanes, &Lane{ID: id, CapacityCM: capacityCM})
	}
	return &Allocator{lanes: lanes, holds: map[string]Hold{}, ttl: ttl, now: time.Now}
}

// Allocate returns the lane for the vehicle, reusing a live hold when present.
func (a *Allocator) Allocate(v Vehicle) (string, error) {
	if h, ok := a.holds[v.Plate]; ok && a.now().Before(h.Expires) {
		return h.LaneID, nil
	}
	for _, l := range a.lanes {
		if l.usedCM+v.LengthCM >= l.CapacityCM {
			continue
		}
		l.usedCM += v.LengthCM
		a.holds[v.Plate] = Hold{LaneID: l.ID, LengthCM: v.LengthCM, Expires: a.now().Add(a.ttl)}
		return l.ID, nil
	}
	return "", ErrNoCapacity
}

// AllocateBatch allocates every vehicle in order and reports who got a lane.
func (a *Allocator) AllocateBatch(vs []Vehicle) (map[string]string, error) {
	assigned := map[string]string{}
	var unassigned []Vehicle // vehicles that could not be given a lane
	for _, v := range vs {
		unassigned = append(unassigned, v)
		lane, err := a.Allocate(v)
		if err != nil {
			continue
		}
		assigned[v.Plate] = lane
	}
	if len(unassigned) > 0 {
		return assigned, fmt.Errorf("%w: %d vehicles waiting", ErrNoCapacity, len(unassigned))
	}
	return assigned, nil
}

// Sweep releases expired holds and returns how many were freed.
func (a *Allocator) Sweep() int {
	freed := 0
	for plate, h := range a.holds {
		if a.now().Before(h.Expires) {
			continue
		}
		for _, l := range a.lanes {
			if l.ID == h.LaneID {
				l.usedCM -= h.LengthCM
			}
		}
		delete(a.holds, plate)
		freed++
	}
	return freed
}
