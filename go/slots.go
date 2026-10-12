package main

import (
	"errors"
	"sort"
)

// ErrUnslotted is returned for a SKU that has no pick slot.
var ErrUnslotted = errors.New("sku has no pick slot")

type Slot struct {
	Aisle int `json:"aisle"`
	Bay   int `json:"bay"`
}

// SlotFinder resolves the pick slot for a SKU. Implementations return
// ErrUnslotted for a SKU with no slot; they never return a zero Slot with a
// nil error.
type SlotFinder interface {
	Find(sku string) (Slot, error)
}

type slotTable map[string]Slot

func (t slotTable) Find(sku string) (Slot, error) {
	s, ok := t[sku]
	if !ok {
		return Slot{}, ErrUnslotted
	}
	return s, nil
}

// PickPath orders a batch's picks by aisle, then bay, so the picker walks
// the zone in a single pass. SKUs without a slot are returned separately.
func PickPath(picks []OrderLine, finder SlotFinder) ([]OrderLine, []string, error) {
	slotOf := make(map[string]Slot, len(picks))
	path := make([]OrderLine, 0, len(picks))
	var unslotted []string
	for _, p := range picks {
		s, err := finder.Find(p.SKU)
		if errors.Is(err, ErrUnslotted) {
			unslotted = append(unslotted, p.SKU)
			continue
		}
		if err != nil {
			return nil, nil, err
		}
		slotOf[p.SKU] = s
		path = append(path, p)
	}
	sort.SliceStable(path, func(i, j int) bool {
		a, b := slotOf[path[i].SKU], slotOf[path[j].SKU]
		if a.Aisle != b.Aisle {
			return a.Aisle < b.Aisle
		}
		return a.Bay < b.Bay
	})
	return path, unslotted, nil
}
