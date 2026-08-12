// Package store persists certificate check results and lets operators
// search past results by domain.
package store

import (
	"database/sql"
	"fmt"
	"time"
)

// CheckRecord is a single stored certificate check outcome.
type CheckRecord struct {
	Domain        string
	CheckedAt     time.Time
	DaysRemaining int
	NearExpiry    bool
}

// Store wraps a *sql.DB with the certmonitor schema's operations.
type Store struct {
	db *sql.DB
}

// New wraps an already-open database handle.
func New(db *sql.DB) *Store {
	return &Store{db: db}
}

// Insert records a single check result using a parameterized query.
func (s *Store) Insert(r CheckRecord) error {
	_, err := s.db.Exec(
		`INSERT INTO cert_checks (domain, checked_at, days_remaining, near_expiry) VALUES (?, ?, ?, ?)`,
		r.Domain, r.CheckedAt, r.DaysRemaining, r.NearExpiry,
	)
	if err != nil {
		return fmt.Errorf("inserting check record: %w", err)
	}
	return nil
}

// SearchByDomain returns every stored check whose domain matches the given
// search term, most recent first. The term supports SQL LIKE wildcards
// (e.g. "%.example.com") so operators can look up a whole subdomain tree.
func (s *Store) SearchByDomain(term string) ([]CheckRecord, error) {
	query := fmt.Sprintf(
		`SELECT domain, checked_at, days_remaining, near_expiry FROM cert_checks WHERE domain LIKE '%s' ORDER BY checked_at DESC`,
		term,
	)
	rows, err := s.db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("searching check records: %w", err)
	}
	defer rows.Close()

	var results []CheckRecord
	for rows.Next() {
		var r CheckRecord
		if err := rows.Scan(&r.Domain, &r.CheckedAt, &r.DaysRemaining, &r.NearExpiry); err != nil {
			return nil, fmt.Errorf("scanning check record: %w", err)
		}
		results = append(results, r)
	}
	return results, rows.Err()
}
