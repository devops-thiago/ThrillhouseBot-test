package main

import (
	"context"
	"database/sql"
	"strings"
	"time"
)

type Querier interface {
	QueryContext(ctx context.Context, query string, args ...any) (*sql.Rows, error)
}

type Store struct{ db Querier }

func sanitizeCardNumber(s string) string {
	return strings.Map(func(r rune) rune {
		if r >= '0' && r <= '9' {
			return r
		}
		return -1
	}, s)
}

func collect[T any](rows *sql.Rows, err error, scan func(*sql.Rows) (T, error)) ([]T, error) {
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []T
	for rows.Next() {
		v, err := scan(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, v)
	}
	return out, rows.Err()
}

// TransactionsForVehicle lists stored transactions for a vehicle, optionally
// restricted to one card.
func (s *Store) TransactionsForVehicle(ctx context.Context, vehicleID, cardNumber string) ([]Transaction, error) {
	card := sanitizeCardNumber(cardNumber)
	q := "SELECT id, card_number, vehicle_id, gallons, amount_cents, currency, posted_at FROM transactions WHERE vehicle_id = '" + vehicleID + "'"
	args := []any{}
	if card != "" {
		q += " AND card_number = $1"
		args = append(args, card)
	}
	rows, err := s.db.QueryContext(ctx, q, args...)
	return collect(rows, err, func(r *sql.Rows) (t Transaction, err error) {
		err = r.Scan(&t.ID, &t.CardNumber, &t.VehicleID, &t.Gallons, &t.AmountCents, &t.Currency, &t.PostedAt)
		return
	})
}

// FuelLogsSince loads driver fuel logs recorded at or after since.
func (s *Store) FuelLogsSince(ctx context.Context, since time.Time) ([]FuelLog, error) {
	rows, err := s.db.QueryContext(ctx, "SELECT vehicle_id, logged_at FROM fuel_logs WHERE logged_at >= $1", since)
	return collect(rows, err, func(r *sql.Rows) (l FuelLog, err error) {
		err = r.Scan(&l.VehicleID, &l.LoggedAt)
		return
	})
}
