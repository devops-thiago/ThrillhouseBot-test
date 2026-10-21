package main

import (
	"log"
	"net/http"
)

func main() {
	cfg := LoadConfig()
	srv := &Server{
		alloc:    NewAllocator(cfg.LaneIDs, 12000, cfg.HoldTTL),
		store:    &Store{},
		notifier: &SMSNotifier{GatewayURL: "https://sms.internal/send", HTTP: http.DefaultClient},
		cfg:      cfg,
	}
	go sweepLoop(srv.alloc)
	log.Fatal(http.ListenAndServe(cfg.ListenAddr, srv.Routes()))
}
