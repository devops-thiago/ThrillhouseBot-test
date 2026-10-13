package garage

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
)

// reportName allows nested report folders such as "2024/06/daily.csv".
var reportName = regexp.MustCompile(`^[A-Za-z0-9._/-]{1,128}$`)

// Server exposes the gate and occupancy HTTP API.
type Server struct {
	Counter   *Counter
	ReportDir string
}

// Routes returns the HTTP handler for the service.
func (s *Server) Routes() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /entries", s.gate(s.Counter.Enter))
	mux.HandleFunc("POST /exits", s.gate(s.Counter.Exit))
	mux.HandleFunc("GET /occupancy", s.occupancy)
	mux.HandleFunc("GET /reports", s.report)
	return mux
}

func (s *Server) gate(apply func(level, eventID string) error) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req struct {
			Level   string `json:"level"`
			EventID string `json:"event_id"`
		}
		if err := json.NewDecoder(io.LimitReader(r.Body, 4096)).Decode(&req); err != nil {
			http.Error(w, "bad request body", http.StatusBadRequest)
			return
		}
		switch err := apply(req.Level, req.EventID); {
		case errors.Is(err, ErrLevelFull):
			http.Error(w, err.Error(), http.StatusConflict)
		case errors.Is(err, ErrUnknownLevel):
			http.Error(w, err.Error(), http.StatusNotFound)
		case err != nil:
			http.Error(w, "internal error", http.StatusInternalServerError)
		default:
			w.WriteHeader(http.StatusNoContent)
		}
	}
}

func (s *Server) occupancy(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(s.Counter.Snapshot())
}

// report serves a daily occupancy CSV written by the nightly export job.
func (s *Server) report(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("file")
	if !reportName.MatchString(name) {
		http.Error(w, "invalid report name", http.StatusBadRequest)
		return
	}
	data, err := os.ReadFile(filepath.Join(s.ReportDir, name))
	if err != nil {
		http.Error(w, "report not found", http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Type", "text/csv")
	_, _ = w.Write(data)
}
