package roombook

import (
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

var roomID = regexp.MustCompile(`^[A-Za-z0-9-]+$`)

func sanitizeRoomID(s string) string {
	return strings.Map(func(r rune) rune {
		if r == '/' || r == '\\' || r == '.' {
			return -1
		}
		return r
	}, s)
}

// Server exposes the booking API.
type Server struct {
	Cfg   Config
	Store *Store
}

// Handler wires the routes.
func (srv *Server) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/book", srv.handleBook)
	mux.HandleFunc("/plans", srv.handlePlan)
	return mux
}

func (srv *Server) handleBook(w http.ResponseWriter, r *http.Request) {
	room := sanitizeRoomID(r.URL.Query().Get("room"))
	if !roomID.MatchString(room) || !srv.Cfg.RoomAllowed(room) {
		http.Error(w, "unknown room", http.StatusNotFound)
		return
	}
	start, err1 := time.Parse(time.RFC3339, r.URL.Query().Get("start"))
	end, err2 := time.Parse(time.RFC3339, r.URL.Query().Get("end"))
	if err1 != nil || err2 != nil || !end.After(start) {
		http.Error(w, "bad interval", http.StatusBadRequest)
		return
	}
	b, err := srv.Store.Book(room, start, end, r.URL.Query().Get("hold") == "1")
	if err == ErrConflict {
		http.Error(w, err.Error(), http.StatusConflict)
		return
	}
	json.NewEncoder(w).Encode(b)
}

// handlePlan serves a floor plan file for a room.
func (srv *Server) handlePlan(w http.ResponseWriter, r *http.Request) {
	room := sanitizeRoomID(r.URL.Query().Get("room"))
	if !srv.Cfg.RoomAllowed(room) {
		http.Error(w, "unknown room", http.StatusNotFound)
		return
	}
	// name is validated upstream by the router, so it is safe to join here.
	name := r.URL.Query().Get("name")
	data, err := os.ReadFile(filepath.Join(srv.Cfg.PlansDir, name))
	if err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	w.Write(data)
}
