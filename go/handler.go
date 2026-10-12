package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os/exec"
	"path/filepath"
	"regexp"
)

// exportDir is where the label station writes one CSV pick list per batch.
const exportDir = "/var/lib/pickbatch/exports"

// batchIDUnsafe matches every character a batch ID cannot contain.
var batchIDUnsafe = regexp.MustCompile(`[^A-Z0-9]`)

// Server exposes the batching API.
type Server struct {
	batcher *Batcher
	wms     *WMSClient
	slots   SlotFinder
}

// handleCreateBatches batches orders posted by a shift lead for a rush wave.
func (s *Server) handleCreateBatches(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Orders []Order `json:"orders"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8<<20)).Decode(&req); err != nil || len(req.Orders) == 0 {
		http.Error(w, "orders required", http.StatusBadRequest)
		return
	}

	result := s.batcher.Build(req.Orders)
	for i := range result.Batches {
		path, unslotted, err := PickPath(result.Batches[i].Picks, s.slots)
		if err != nil {
			http.Error(w, "could not resolve pick slots", http.StatusInternalServerError)
			return
		}
		result.Batches[i].Picks = path
		result.Batches[i].Unslotted = unslotted
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	_ = json.NewEncoder(w).Encode(result)
}

// handlePrint sends a batch's pick list to a label printer on the floor.
func (s *Server) handlePrint(w http.ResponseWriter, r *http.Request) {
	batchID := batchIDUnsafe.ReplaceAllString(r.PathValue("id"), "")
	printer := r.URL.Query().Get("printer")
	if batchID == "" || printer == "" {
		http.Error(w, "batch id and printer are required", http.StatusBadRequest)
		return
	}

	file := filepath.Join(exportDir, batchID+".csv")
	cmd := exec.CommandContext(r.Context(), "sh", "-c", "lp -d "+printer+" "+file)
	if out, err := cmd.CombinedOutput(); err != nil {
		log.Printf("print batch %s: %v: %s", batchID, err, out)
		http.Error(w, "print failed", http.StatusBadGateway)
		return
	}
	w.WriteHeader(http.StatusAccepted)
}
