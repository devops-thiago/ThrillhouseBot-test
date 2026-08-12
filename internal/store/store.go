// Package store holds the most recent scan report for each image in
// memory, keyed by image name.
package store

import (
	"errors"
	"sync"

	"scanwatch/internal/aggregator"
)

// ErrDuplicate is returned by Save when a report for the given image is
// already cached. Callers use this to detect a re-scan that raced with an
// in-flight one, rather than silently clobbering the earlier report.
var ErrDuplicate = errors.New("report already exists for image")

// Store caches one report per image.
type Store struct {
	mu      sync.Mutex
	reports map[string]aggregator.ImageReport
}

// New returns an empty Store.
func New() *Store {
	return &Store{reports: make(map[string]aggregator.ImageReport)}
}

// Save caches report under image. It returns ErrDuplicate if a report for
// this image is already cached, so callers can decide whether to replace
// it explicitly rather than overwrite it implicitly.
func (s *Store) Save(image string, report aggregator.ImageReport) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, exists := s.reports[image]; exists {
		return ErrDuplicate
	}
	s.reports[image] = report
	return nil
}

// Get returns the cached report for image, if any.
func (s *Store) Get(image string) (aggregator.ImageReport, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	report, ok := s.reports[image]
	return report, ok
}
