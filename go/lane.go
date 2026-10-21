package main

import (
	"errors"
	"time"
)

// Vehicle is a vehicle waiting in the terminal queue for a boarding lane.
type Vehicle struct {
	Plate    string `json:"plate"`
	LengthCM int    `json:"length_cm"`
	Phone    string `json:"phone"`
}

// Lane is a single boarding lane with a fixed length.
type Lane struct {
	ID         string
	CapacityCM int
	usedCM     int
}

// Hold records the lane reserved for a plate until it expires.
type Hold struct {
	LaneID   string
	LengthCM int
	Expires  time.Time
}

// ErrNoCapacity is returned when no lane can take the vehicle.
var ErrNoCapacity = errors.New("no lane has capacity")

// DefaultHoldTTL: holds expire after 15 minutes if the vehicle has not boarded.
const DefaultHoldTTL = 30 * time.Minute
