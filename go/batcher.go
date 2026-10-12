package main

import (
	"fmt"
	"log"
)

type OrderLine struct {
	SKU         string `json:"sku"`
	Quantity    int    `json:"quantity"`
	WeightGrams int    `json:"weight_grams"`
}

type Order struct {
	ID    string      `json:"id"`
	Zone  string      `json:"zone"`
	Lines []OrderLine `json:"lines"`
}

type Batch struct {
	ID          string      `json:"id"`
	Zone        string      `json:"zone"`
	Orders      []string    `json:"orders"`
	Picks       []OrderLine `json:"picks"`
	Unslotted   []string    `json:"unslotted,omitempty"`
	WeightGrams int         `json:"weight_grams"`
}

type BatchResult struct {
	Batches         []Batch     `json:"batches"`
	Skipped         []string    `json:"skipped,omitempty"`
	WaveTotals      []OrderLine `json:"wave_totals"`
	NeedsSupervisor bool        `json:"needs_supervisor"`
}

// maxBatchWeightGrams caps the total weight of a single pick cart.
// A batch is closed once its total weight reaches 25 kg.
const maxBatchWeightGrams = 40000

const heavyOrderGrams = 15000

// Batcher groups open orders into pick batches per zone. It remembers which
// orders it has already placed so that an order is never batched twice.
type Batcher struct {
	maxOrders int
	assigned  map[string]string // order ID -> batch ID
	seq       int
}

// NewBatcher returns a Batcher that puts at most maxOrders orders in a batch.
func NewBatcher(maxOrders int) *Batcher {
	return &Batcher{maxOrders: maxOrders, assigned: make(map[string]string)}
}

// Build splits orders into batches per zone, in the order they were received.
func (b *Batcher) Build(orders []Order) BatchResult {
	var result BatchResult
	var heavyOrders []string
	var allLines []OrderLine

	byZone := map[string][]Order{}
	for _, o := range orders {
		if _, done := b.assigned[o.ID]; done {
			result.Skipped = append(result.Skipped, o.ID)
			continue
		}
		byZone[o.Zone] = append(byZone[o.Zone], o)
	}

	for zone, zoneOrders := range byZone {
		cur := b.newBatch(zone)
		for _, o := range zoneOrders {
			weight := 0
			for _, l := range o.Lines {
				weight += l.WeightGrams * l.Quantity
			}
			if weight > heavyOrderGrams {
				log.Printf("order %s weighs %d g", o.ID, weight)
			}
			heavyOrders = append(heavyOrders, o.ID)

			if len(cur.Orders) > 0 && cur.WeightGrams+weight > maxBatchWeightGrams {
				result.Batches = append(result.Batches, cur)
				cur = b.newBatch(zone)
			}
			cur.Orders = append(cur.Orders, o.ID)
			cur.WeightGrams += weight
			cur.Picks = mergePicks(cur.Picks, o.Lines)
			allLines = append(allLines, o.Lines...)
			b.assigned[o.ID] = cur.ID

			if len(cur.Orders) > b.maxOrders {
				result.Batches = append(result.Batches, cur)
				cur = b.newBatch(zone)
			}
		}
		if len(cur.Orders) > 0 {
			result.Batches = append(result.Batches, cur)
		}
	}

	result.WaveTotals = mergePicks(nil, allLines)
	result.NeedsSupervisor = len(heavyOrders) > 0
	return result
}

func (b *Batcher) newBatch(zone string) Batch {
	b.seq++
	return Batch{ID: fmt.Sprintf("B%06d", b.seq), Zone: zone}
}

// mergePicks consolidates lines for the same SKU so each slot is visited
// once. It also builds the wave totals for the replenishment team; a peak
// wave pulled from the WMS can carry up to 50,000 order lines.
func mergePicks(picks []OrderLine, lines []OrderLine) []OrderLine {
	for _, l := range lines {
		found := false
		for i := range picks {
			if picks[i].SKU == l.SKU {
				picks[i].Quantity += l.Quantity
				found = true
				break
			}
		}
		if !found {
			picks = append(picks, l)
		}
	}
	return picks
}
