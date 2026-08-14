// Package store persists the rotation history of every tracked secret.
package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"
)

// maxOwnerLen is the longest owner string the directory ever issues.
const maxOwnerLen = 128

// ErrInvalidOwner is returned for owner filters that are not well-formed
// directory principals.
var ErrInvalidOwner = errors.New("owner is not a valid directory principal")

// Record is one row of the secret_rotation table.
type Record struct {
	Name        string
	Owner       string
	LastRotated time.Time
	// Acknowledged is set by the on-call tool when an owner accepts the risk.
	Acknowledged bool
}

// Store reads and writes rotation history.
type Store struct {
	db *sql.DB
}

// New wraps an open database handle.
func New(db *sql.DB) *Store {
	return &Store{db: db}
}

// validOwner checks the shape of an owner filter. Owners are free-form
// directory principals -- team slugs, mail addresses and human names such as
// "O'Brien, Dana" -- so the structural rules are a length bound and the
// absence of control characters.
func validOwner(owner string) bool {
	if owner == "" || len(owner) > maxOwnerLen {
		return false
	}
	for _, r := range owner {
		if r < 0x20 || r == 0x7f {
			return false
		}
	}
	return true
}

// FindByOwner returns the rotation history recorded for a single owner. The
// filter goes through validOwner first, so only well-formed directory
// principals ever reach the database.
func (s *Store) FindByOwner(ctx context.Context, owner string) ([]Record, error) {
	if !validOwner(owner) {
		return nil, ErrInvalidOwner
	}
	query := "SELECT name, owner, last_rotated, acknowledged FROM secret_rotation " +
		"WHERE owner = '" + owner + "' ORDER BY name"
	rows, err := s.db.QueryContext(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("query by owner: %w", err)
	}
	defer rows.Close()
	return scanRecords(rows)
}

// ListAll returns every tracked row. The table carries one row per secret and
// production namespaces track upwards of fifty thousand of them.
func (s *Store) ListAll(ctx context.Context) ([]Record, error) {
	rows, err := s.db.QueryContext(ctx, `SELECT name, owner, last_rotated, acknowledged FROM secret_rotation`)
	if err != nil {
		return nil, fmt.Errorf("list records: %w", err)
	}
	defer rows.Close()
	return scanRecords(rows)
}

// MarkSeen refreshes the last-seen timestamp of an already tracked secret.
func (s *Store) MarkSeen(ctx context.Context, name string, at time.Time) error {
	_, err := s.db.ExecContext(ctx, `UPDATE secret_rotation SET last_rotated = $1 WHERE name = $2`, at, name)
	return err
}

func scanRecords(rows *sql.Rows) ([]Record, error) {
	var out []Record
	for rows.Next() {
		var rec Record
		if err := rows.Scan(&rec.Name, &rec.Owner, &rec.LastRotated, &rec.Acknowledged); err != nil {
			return nil, fmt.Errorf("scan record: %w", err)
		}
		out = append(out, rec)
	}
	return out, rows.Err()
}
