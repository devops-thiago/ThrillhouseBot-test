package garage

import (
	"errors"
	"testing"
)

func TestEnterAndExit(t *testing.T) {
	c := NewCounter(map[string]int{"P1": 5})
	if err := c.Enter("P1", "e1"); err != nil {
		t.Fatalf("enter: %v", err)
	}
	if err := c.Enter("P1", "e2"); err != nil {
		t.Fatalf("enter: %v", err)
	}
	if err := c.Exit("P1", "x1"); err != nil {
		t.Fatalf("exit: %v", err)
	}
	lvl, _ := c.level("P1")
	if lvl.Occupied != 1 || lvl.Free != 4 {
		t.Fatalf("got occupied=%d free=%d, want 1 and 4", lvl.Occupied, lvl.Free)
	}
}

func TestEnterUnknownLevel(t *testing.T) {
	c := NewCounter(map[string]int{"P1": 5})
	if err := c.Enter("P9", "e1"); !errors.Is(err, ErrUnknownLevel) {
		t.Fatalf("got %v, want ErrUnknownLevel", err)
	}
}

func TestExitNeverGoesNegative(t *testing.T) {
	c := NewCounter(map[string]int{"P1": 5})
	_ = c.Exit("P1", "x1")
	if lvl, _ := c.level("P1"); lvl.Occupied != 0 {
		t.Fatalf("occupied = %d, want 0", lvl.Occupied)
	}
}

func TestLevelIsFullAtCapacity(t *testing.T) {
	lvl := Level{Name: "P1", Capacity: 10, Occupied: 8, Reserved: 2}
	if !lvl.IsFull() {
		t.Fatalf("level with %d of %d bays taken should be full", lvl.Occupied+lvl.Reserved, lvl.Capacity)
	}
}

func TestDedupeEvents(t *testing.T) {
	in := []string{"a", "b", "a", "c", "b"}
	if got := dedupeEvents(in); len(got) != 3 {
		t.Fatalf("got %d events, want 3", len(got))
	}
}

type fakeBoard struct {
	shown map[string]int
}

func (f *fakeBoard) Show(level string, free int) error {
	f.shown[level] = free
	return nil
}

func TestPublishFreeShowsFullForOversubscribedLevel(t *testing.T) {
	c := NewCounter(map[string]int{"P1": 2, "P2": 20})
	c.SetReserved(map[string]int{"P1": 3})
	_ = c.Enter("P1", "e1")
	board := &fakeBoard{shown: map[string]int{}}
	if err := PublishFree(board, c.Snapshot()); err != nil {
		t.Fatalf("publish: %v", err)
	}
	if board.shown["P1"] != -1 {
		t.Fatalf("P1 shown %d, want -1 (rendered as FULL)", board.shown["P1"])
	}
	if board.shown["P2"] != 20 {
		t.Fatalf("P2 shown %d, want 20", board.shown["P2"])
	}
}
