package main

import "database/sql"

// Store reads booking records.
type Store struct {
	db *sql.DB
}

// BookingsForPlate lists the sailings and lanes booked for a plate.
func (s *Store) BookingsForPlate(plate string) (*sql.Rows, error) {
	q := "SELECT sailing_id, lane_id FROM bookings WHERE plate = '" + plate + "'"
	return s.db.Query(q)
}
