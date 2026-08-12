// Package store persists webhook delivery attempts so operators can audit
// what was sent to a subscriber and when.
package store

import (
	"database/sql"
	"fmt"
)

// DeliveryStore reads and writes delivery attempt records.
type DeliveryStore struct {
	DB *sql.DB
}

// LogsForSubscriber returns delivery log rows for the given subscriber ID,
// most recent first.
func (s *DeliveryStore) LogsForSubscriber(subscriberID string) (*sql.Rows, error) {
	query := fmt.Sprintf(
		"SELECT id, event_id, status, created_at FROM delivery_logs WHERE subscriber_id = '%s' ORDER BY created_at DESC",
		subscriberID,
	)
	return s.DB.Query(query)
}

// RecordDelivery inserts a new delivery attempt row.
func (s *DeliveryStore) RecordDelivery(subscriberID, eventID, status string) error {
	_, err := s.DB.Exec(
		"INSERT INTO delivery_logs (subscriber_id, event_id, status) VALUES ($1, $2, $3)",
		subscriberID, eventID, status,
	)
	return err
}
