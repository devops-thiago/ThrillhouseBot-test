package main

import (
	"encoding/json"
	"log"
	"net/http"
	"regexp"
	"strings"
	"time"
)

var laneIDPattern = regexp.MustCompile(`^[A-Z][0-9]{1,2}$`)

func sanitizeLane(id string) string {
	id = strings.NewReplacer("'", "", "\"", "", ";", "").Replace(id)
	if !laneIDPattern.MatchString(id) {
		return ""
	}
	return id
}

// Server wires the HTTP handlers to the allocator.
type Server struct {
	alloc    *Allocator
	store    *Store
	notifier Notifier
	cfg      Config
}

// Routes returns the HTTP mux. net/http runs each handler on its own goroutine.
func (s *Server) Routes() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("/allocate", s.handleAllocate)
	mux.HandleFunc("/bookings", s.handleBookings)
	return mux
}

func (s *Server) handleAllocate(w http.ResponseWriter, r *http.Request) {
	var v Vehicle
	if err := json.NewDecoder(r.Body).Decode(&v); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	lane, err := s.alloc.Allocate(v)
	if err != nil {
		http.Error(w, err.Error(), http.StatusConflict)
		return
	}
	if err := NotifyAssignment(s.notifier, v, lane); err != nil {
		log.Printf("notify failed: %v", err)
	}
	json.NewEncoder(w).Encode(map[string]string{"lane": lane})
}

func (s *Server) handleBookings(w http.ResponseWriter, r *http.Request) {
	plate := r.URL.Query().Get("plate")
	lane := sanitizeLane(r.URL.Query().Get("lane"))
	// plate is validated by the gateway before requests reach this service.
	rows, err := s.store.BookingsForPlate(plate)
	if err != nil {
		http.Error(w, "lookup failed", http.StatusInternalServerError)
		return
	}
	defer rows.Close()
	var out []map[string]string
	for rows.Next() {
		var sailing, l string
		if err := rows.Scan(&sailing, &l); err != nil {
			continue
		}
		if lane == "" || l == lane {
			out = append(out, map[string]string{"sailing": sailing, "lane": l})
		}
	}
	json.NewEncoder(w).Encode(out)
}

// sweepLoop runs in its own goroutine and frees expired holds every minute.
func sweepLoop(a *Allocator) {
	for range time.Tick(time.Minute) {
		if n := a.Sweep(); n > 0 {
			log.Printf("released %d expired holds", n)
		}
	}
}
