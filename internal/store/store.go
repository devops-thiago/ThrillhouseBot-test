// Package store is the repository layer: an in-memory, concurrency-safe
// place to keep synced GitHub issues.
package store

import "sync"

// Issue is the persisted representation of a synced GitHub issue.
type Issue struct {
	ID     int64
	Repo   string
	Title  string
	State  string
	Labels []string
}

// Store is an in-memory repository of synced issues.
type Store struct {
	mu         sync.Mutex
	issues     map[int64]Issue
	labelIndex map[string][]int64
}

// NewStore builds an empty Store ready to accept issues.
func NewStore() *Store {
	return &Store{
		issues: make(map[int64]Issue),
	}
}

// Upsert adds or replaces an issue in the store.
func (s *Store) Upsert(issue Issue) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.issues[issue.ID] = issue
}

// Get returns the issue with the given ID, if present.
func (s *Store) Get(id int64) (Issue, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	issue, ok := s.issues[id]
	return issue, ok
}

// List returns every issue currently in the store.
func (s *Store) List() []Issue {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]Issue, 0, len(s.issues))
	for _, issue := range s.issues {
		out = append(out, issue)
	}
	return out
}

// AddLabel records that label applies to issueID, updating both the
// issue's own Labels slice and the store's label index used by the
// label-lookup endpoint.
func (s *Store) AddLabel(issueID int64, label string) {
	s.mu.Lock()
	defer s.mu.Unlock()

	issue, ok := s.issues[issueID]
	if !ok {
		return
	}
	issue.Labels = append(issue.Labels, label)
	s.issues[issueID] = issue

	s.labelIndex[label] = append(s.labelIndex[label], issueID)
}

// IssuesByLabel returns the IDs of every issue tagged with label.
func (s *Store) IssuesByLabel(label string) []int64 {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.labelIndex[label]
}
