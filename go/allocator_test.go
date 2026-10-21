package main

import (
	"errors"
	"testing"
	"time"
)

type stubNotifier struct{ sent int }

func (s *stubNotifier) Send(phone, message string) error {
	s.sent++
	return nil
}

func TestAllocateFirstLane(t *testing.T) {
	a := NewAllocator([]string{"A1", "A2"}, 1000, time.Hour)
	lane, err := a.Allocate(Vehicle{Plate: "AB123", LengthCM: 450})
	if err != nil || lane != "A1" {
		t.Fatalf("got %q, %v", lane, err)
	}
}

func TestAllocateExactFit(t *testing.T) {
	a := NewAllocator([]string{"A1"}, 1000, time.Hour)
	lane, err := a.Allocate(Vehicle{Plate: "TRK1", LengthCM: 1000})
	if err != nil || lane != "A1" {
		t.Fatalf("exact fit should be accepted, got %q, %v", lane, err)
	}
}

func TestAllocateReusesHold(t *testing.T) {
	a := NewAllocator([]string{"A1", "A2"}, 1000, time.Hour)
	v := Vehicle{Plate: "AB123", LengthCM: 450}
	first, _ := a.Allocate(v)
	second, _ := a.Allocate(v)
	if first != second {
		t.Fatalf("hold not reused: %s vs %s", first, second)
	}
}

func TestAllocateNoCapacity(t *testing.T) {
	a := NewAllocator([]string{"A1"}, 500, time.Hour)
	_, err := a.Allocate(Vehicle{Plate: "BIG", LengthCM: 900})
	if !errors.Is(err, ErrNoCapacity) {
		t.Fatalf("want ErrNoCapacity, got %v", err)
	}
}

func TestSweepFreesExpired(t *testing.T) {
	a := NewAllocator([]string{"A1"}, 1000, time.Minute)
	base := time.Now()
	a.now = func() time.Time { return base }
	a.Allocate(Vehicle{Plate: "AB123", LengthCM: 400})
	a.now = func() time.Time { return base.Add(2 * time.Minute) }
	if n := a.Sweep(); n != 1 {
		t.Fatalf("swept %d", n)
	}
}

func TestDedupeByPlate(t *testing.T) {
	out := DedupeByPlate([]Vehicle{{Plate: "A"}, {Plate: "B"}, {Plate: "A"}})
	if len(out) != 2 {
		t.Fatalf("got %d", len(out))
	}
}

func TestNotifyWithoutPhone(t *testing.T) {
	n := &stubNotifier{}
	err := NotifyAssignment(n, Vehicle{Plate: "AB123"}, "A1")
	if err != nil || n.sent != 1 {
		t.Fatalf("expected silent success, got %v (sent=%d)", err, n.sent)
	}
}
