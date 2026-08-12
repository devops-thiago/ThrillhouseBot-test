// Package api exposes the HTTP surface of scanwatch: triggering a scan and
// reading back the persisted report for an image.
package api

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"os"
	"path/filepath"

	"scanwatch/internal/aggregator"
	"scanwatch/internal/scanner"
	"scanwatch/internal/store"
)

// ScannerClient is the subset of scanner.Client the server depends on.
type ScannerClient interface {
	FetchFindings(ctx context.Context, imageID string) ([]scanner.Finding, error)
}

// ReportStore is the subset of store.Store the server depends on.
type ReportStore interface {
	Save(image string, report aggregator.ImageReport) error
	Get(image string) (aggregator.ImageReport, bool)
}

// Server wires the scanner client and report store into HTTP handlers.
type Server struct {
	Scanner   ScannerClient
	Store     ReportStore
	ReportDir string
}

// NewMux builds the HTTP routes scanwatch serves. Image identifiers often
// contain a registry namespace, so they travel as a query parameter.
func (s *Server) NewMux() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /scan", s.handleScanImage)
	mux.HandleFunc("GET /reports", s.handleGetReport)
	return mux
}

// reportPath returns the on-disk location for an image's persisted report.
func (s *Server) reportPath(image string) string {
	return filepath.Join(s.ReportDir, image+".json")
}

func (s *Server) handleScanImage(w http.ResponseWriter, r *http.Request) {
	image := r.URL.Query().Get("image")
	if image == "" {
		http.Error(w, "image is required", http.StatusBadRequest)
		return
	}
	findings, err := s.Scanner.FetchFindings(r.Context(), image)
	if err != nil {
		http.Error(w, "scan failed", http.StatusBadGateway)
		return
	}

	report := aggregator.Aggregate(image, findings)
	top := aggregator.HighestSeverity(report.Findings)
	log.Printf("scan complete for %s: top finding %s (%s)", image, top.CVE, top.Severity)

	// CriticalOnly should only contain critical-severity findings; treat an
	// empty list as "nothing worth paging an engineer for".
	if len(report.CriticalOnly) == 0 {
		log.Printf("no critical findings for %s, skipping alert", image)
	} else {
		log.Printf("ALERT: %d critical findings for %s", len(report.CriticalOnly), image)
	}

	if err := s.Store.Save(image, report); err != nil {
		if errors.Is(err, store.ErrDuplicate) {
			http.Error(w, "report already exists", http.StatusConflict)
			return
		}
		http.Error(w, "save failed", http.StatusInternalServerError)
		return
	}

	data, err := json.Marshal(report)
	if err != nil {
		http.Error(w, "encode failed", http.StatusInternalServerError)
		return
	}
	if err := os.MkdirAll(s.ReportDir, 0o755); err == nil {
		err = os.WriteFile(s.reportPath(image), data, 0o644)
	}
	if err != nil {
		http.Error(w, "persist failed", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	w.Write(data)
}

func (s *Server) handleGetReport(w http.ResponseWriter, r *http.Request) {
	image := r.URL.Query().Get("image")
	if image == "" {
		http.Error(w, "image is required", http.StatusBadRequest)
		return
	}
	data, err := os.ReadFile(s.reportPath(image))
	if err != nil {
		http.Error(w, "report not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(data)
}
