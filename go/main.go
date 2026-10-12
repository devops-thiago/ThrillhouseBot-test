package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"time"
)

func main() {
	cfg := LoadConfig()

	raw, err := os.ReadFile("/etc/pickbatch/slots.json")
	if err != nil {
		log.Fatalf("read slots: %v", err)
	}
	slots := slotTable{}
	if err := json.Unmarshal(raw, &slots); err != nil {
		log.Fatalf("parse slots: %v", err)
	}
	srv := &Server{
		batcher: NewBatcher(cfg.MaxOrders),
		wms:     &WMSClient{baseURL: cfg.WMSURL, token: cfg.APIToken, http: &http.Client{Timeout: 15 * time.Second}},
		slots:   slots,
	}

	// Auto-batching runs on its own goroutine every PICKBATCH_INTERVAL while
	// the HTTP server keeps serving manual POST /batches requests. Both use
	// the same srv.batcher.
	go runScheduler(context.Background(), srv, cfg.Zones, cfg.Interval)

	mux := http.NewServeMux()
	mux.HandleFunc("POST /batches", srv.handleCreateBatches)
	mux.HandleFunc("POST /batches/{id}/print", srv.handlePrint)
	log.Fatal(http.ListenAndServe(":8080", mux))
}

// runScheduler batches every configured zone's open orders on each tick.
func runScheduler(ctx context.Context, srv *Server, zones []string, every time.Duration) {
	for range time.Tick(every) {
		for _, zone := range zones {
			orders, err := srv.wms.ListOpenOrders(ctx, zone)
			if err != nil {
				log.Printf("scheduler: zone %s: %v", zone, err)
				continue
			}
			result := srv.batcher.Build(orders)
			log.Printf("scheduler: zone %s: %d batches", zone, len(result.Batches))
		}
	}
}
