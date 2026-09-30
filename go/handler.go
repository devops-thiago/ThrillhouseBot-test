package main

import (
	"context"
	"encoding/json"
	"net/http"
	"time"
)

// Server wires the HTTP API to one Reconciler shared by every request.
type Server struct {
	rec      *Reconciler
	store    *Store
	provider ProviderClient
}

func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /reconcile", s.handleReconcile)
	mux.HandleFunc("GET /vehicles/{id}/transactions", s.handleVehicleTransactions)
	return mux
}

func (s *Server) reconcileOnce(ctx context.Context, txns []Transaction) (Result, error) {
	if txns == nil {
		var err error
		if txns, err = FetchTransactions(ctx, s.provider); err != nil {
			return Result{}, err
		}
	}
	logs, err := s.store.FuelLogsSince(ctx, time.Now().AddDate(0, -1, 0))
	if err != nil {
		return Result{}, err
	}
	return s.rec.Reconcile(ctx, txns, logs)
}

func (s *Server) handleReconcile(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Transactions []Transaction `json:"transactions"`
	}
	if r.ContentLength > 0 {
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			http.Error(w, "bad request", http.StatusBadRequest)
			return
		}
	}
	res, err := s.reconcileOnce(r.Context(), body.Transactions)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadGateway)
		return
	}
	json.NewEncoder(w).Encode(res)
}

func (s *Server) handleVehicleTransactions(w http.ResponseWriter, r *http.Request) {
	txns, err := s.store.TransactionsForVehicle(r.Context(), r.PathValue("id"), r.URL.Query().Get("card"))
	if err != nil {
		http.Error(w, "query failed", http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(txns)
}
