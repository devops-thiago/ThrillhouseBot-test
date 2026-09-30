package main

import (
	"log"
	"net/http"
	"time"

	"example.com/roombook"
)

func main() {
	cfg := roombook.LoadConfig()
	store := roombook.NewStore()

	go func() {
		for range time.Tick(time.Minute) {
			n := store.ExpireHolds(time.Now(), cfg.HoldTTL)
			log.Printf("expired %d holds", n)
		}
	}()

	srv := &roombook.Server{Cfg: cfg, Store: store}
	log.Fatal(http.ListenAndServe(cfg.Addr, srv.Handler()))
}
