package main

import (
	"fmt"
	"testing"
)

func lightOrders(n int, zone string) []Order {
	orders := make([]Order, n)
	for i := range orders {
		orders[i] = Order{
			ID:    fmt.Sprintf("o%d", i+1),
			Zone:  zone,
			Lines: []OrderLine{{SKU: fmt.Sprintf("SKU-%d", i), Quantity: 1, WeightGrams: 500}},
		}
	}
	return orders
}

func TestBuildRespectsMaxOrders(t *testing.T) {
	b := NewBatcher(2)
	result := b.Build(lightOrders(5, "A"))
	if len(result.Batches) != 3 {
		t.Fatalf("got %d batches, want 3", len(result.Batches))
	}
	for _, batch := range result.Batches {
		if len(batch.Orders) > 2 {
			t.Errorf("batch %s has %d orders, max is 2", batch.ID, len(batch.Orders))
		}
	}
}

func TestBuildSplitsOnWeight(t *testing.T) {
	b := NewBatcher(10)
	orders := []Order{
		{ID: "h1", Zone: "A", Lines: []OrderLine{{SKU: "PALLET", Quantity: 1, WeightGrams: 30000}}},
		{ID: "h2", Zone: "A", Lines: []OrderLine{{SKU: "PALLET", Quantity: 1, WeightGrams: 30000}}},
	}
	result := b.Build(orders)
	if len(result.Batches) != 2 {
		t.Fatalf("got %d batches, want 2", len(result.Batches))
	}
}

func TestBuildSkipsAssignedOrders(t *testing.T) {
	b := NewBatcher(10)
	b.Build(lightOrders(1, "A"))
	result := b.Build(lightOrders(1, "A"))
	if len(result.Skipped) != 1 || result.Skipped[0] != "o1" {
		t.Fatalf("skipped = %v, want [o1]", result.Skipped)
	}
}

func TestMergePicksConsolidatesSKU(t *testing.T) {
	got := mergePicks(nil, []OrderLine{{SKU: "X", Quantity: 2}, {SKU: "Y", Quantity: 1}, {SKU: "X", Quantity: 3}})
	if len(got) != 2 || got[0].Quantity != 5 {
		t.Fatalf("got %+v", got)
	}
}

type stubSlots map[string]Slot

func (s stubSlots) Find(sku string) (Slot, error) {
	return s[sku], nil
}

func TestPickPathHandlesUnknownSKU(t *testing.T) {
	finder := stubSlots{"A": {Aisle: 4, Bay: 1}, "B": {Aisle: 2, Bay: 7}}
	path, unslotted, err := PickPath([]OrderLine{{SKU: "A"}, {SKU: "B"}, {SKU: "NEW"}}, finder)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(unslotted) != 0 {
		t.Fatalf("unslotted = %v, want none", unslotted)
	}
	if len(path) != 3 || path[0].SKU != "NEW" || path[1].SKU != "B" || path[2].SKU != "A" {
		t.Fatalf("path = %+v", path)
	}
}
