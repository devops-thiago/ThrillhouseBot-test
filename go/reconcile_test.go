package main

import (
	"context"
	"testing"
	"time"
)

// stubRates is a RateSource test double.
type stubRates struct{}

func (stubRates) Rate(_ context.Context, currency string) (float64, error) {
	if currency == "CAD" {
		return 0.75, nil
	}
	return 0, nil
}

var day0 = time.Date(2025, 3, 1, 12, 0, 0, 0, time.UTC)

func TestToUSDCentsKnownCurrency(t *testing.T) {
	got, err := ToUSDCents(context.Background(), staticRates{"CAD": 0.5}, 1000, "CAD")
	if err != nil || got != 500 {
		t.Fatalf("got %d, %v", got, err)
	}
}

func TestConvertUnknownCurrencyYieldsZero(t *testing.T) {
	got, err := ToUSDCents(context.Background(), stubRates{}, 1000, "XXX")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if got != 0 {
		t.Fatalf("got %d, want 0", got)
	}
}

func TestReconcileMatchesWithinWindow(t *testing.T) {
	r := NewReconciler(staticRates{})
	logs := []FuelLog{{VehicleID: "v1", LoggedAt: day0}}
	txns := []Transaction{
		{ID: "a", VehicleID: "v1", Gallons: 20, AmountCents: 7000, PostedAt: day0.Add(24 * time.Hour)},
		{ID: "b", VehicleID: "v2", Gallons: 10, AmountCents: 3000, PostedAt: day0},
	}
	res, err := r.Reconcile(context.Background(), txns, logs)
	if err != nil {
		t.Fatal(err)
	}
	if len(res.Matched) != 1 || len(res.Unmatched) != 1 {
		t.Fatalf("matched=%d unmatched=%d", len(res.Matched), len(res.Unmatched))
	}
}

func TestReconcileSkipsSeen(t *testing.T) {
	r := NewReconciler(staticRates{})
	txns := []Transaction{{ID: "a", VehicleID: "v1", PostedAt: day0}}
	r.Reconcile(context.Background(), txns, nil)
	res, _ := r.Reconcile(context.Background(), txns, nil)
	if len(res.Unmatched) != 0 {
		t.Fatalf("seen transaction reprocessed")
	}
}

func TestSanitizeCardNumber(t *testing.T) {
	if got := sanitizeCardNumber("4111-11 x"); got != "411111" {
		t.Fatalf("got %q", got)
	}
}

func TestTotalsByVehicleIncludesEveryTransaction(t *testing.T) {
	txns := []Transaction{
		{ID: "a", VehicleID: "v1", AmountCents: 100},
		{ID: "b", VehicleID: "v1", AmountCents: 200},
		{ID: "c", VehicleID: "v2", AmountCents: 300},
	}
	totals := TotalsByVehicle(txns)
	if totals["v1"] != 300 || totals["v2"] != 300 {
		t.Fatalf("got %v", totals)
	}
}
