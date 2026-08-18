// Package store persists the expiry state of every tracked endpoint and the
// waivers that suppress it.
package store

import (
	"context"
	"database/sql"
	"fmt"
	"time"
)

// State is one row of the certificate_state table: what the last scan
// concluded about a host.
type State struct {
	Host      string
	Owner     string
	Serial    string
	NotAfter  time.Time
	Severity  string
	CheckedAt time.Time
}

// Waiver suppresses reporting for a host until Until has passed. Operations
// files them while a migration is in flight.
type Waiver struct {
	Host   string
	Until  time.Time
	Reason string
}

// Store reads and writes the scan state.
type Store struct {
	db *sql.DB
}

// New wraps an open database handle.
func New(db *sql.DB) *Store {
	return &Store{db: db}
}

// ActiveWaivers returns the waivers that have not expired at asOf.
func (s *Store) ActiveWaivers(ctx context.Context, asOf time.Time) ([]Waiver, error) {
	rows, err := s.db.QueryContext(ctx,
		`SELECT host, waived_until, reason FROM certificate_waivers WHERE waived_until > $1`, asOf)
	if err != nil {
		return nil, fmt.Errorf("query waivers: %w", err)
	}
	defer rows.Close()

	var out []Waiver
	for rows.Next() {
		var w Waiver
		if err := rows.Scan(&w.Host, &w.Until, &w.Reason); err != nil {
			return nil, fmt.Errorf("scan waiver: %w", err)
		}
		out = append(out, w)
	}
	return out, rows.Err()
}

// SaveState upserts the outcome of a scan for a single host.
func (s *Store) SaveState(ctx context.Context, state State) error {
	_, err := s.db.ExecContext(ctx,
		`INSERT INTO certificate_state (host, owner, serial, not_after, severity, checked_at)
		 VALUES ($1, $2, $3, $4, $5, $6)
		 ON CONFLICT (host) DO UPDATE SET
		   owner = EXCLUDED.owner,
		   serial = EXCLUDED.serial,
		   not_after = EXCLUDED.not_after,
		   severity = EXCLUDED.severity,
		   checked_at = EXCLUDED.checked_at`,
		state.Host, state.Owner, state.Serial, state.NotAfter, state.Severity, state.CheckedAt)
	if err != nil {
		return fmt.Errorf("save state for %s: %w", state.Host, err)
	}
	return nil
}

// StatesByOwner returns the recorded state for one owner, soonest expiry
// first. An empty owner returns no rows rather than the whole table.
func (s *Store) StatesByOwner(ctx context.Context, owner string) ([]State, error) {
	if owner == "" {
		return nil, nil
	}
	rows, err := s.db.QueryContext(ctx,
		`SELECT host, owner, serial, not_after, severity, checked_at
		 FROM certificate_state WHERE owner = $1 ORDER BY not_after`, owner)
	if err != nil {
		return nil, fmt.Errorf("query by owner: %w", err)
	}
	defer rows.Close()

	var out []State
	for rows.Next() {
		var st State
		if err := rows.Scan(&st.Host, &st.Owner, &st.Serial, &st.NotAfter, &st.Severity, &st.CheckedAt); err != nil {
			return nil, fmt.Errorf("scan state: %w", err)
		}
		out = append(out, st)
	}
	return out, rows.Err()
}
