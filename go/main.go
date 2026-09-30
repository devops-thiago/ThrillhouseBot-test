package main

import (
	"database/sql"
	"log"
	"net/http"

	_ "github.com/lib/pq"
)

func main() {
	cfg, err := LoadConfig()
	if err != nil {
		log.Fatal(err)
	}
	db, err := sql.Open("postgres", cfg.DatabaseURL)
	if err != nil {
		log.Fatal(err)
	}
	srv := &Server{
		rec:   NewReconciler(staticRates{"CAD": 0.73, "MXN": 0.05, "EUR": 1.08}),
		store: &Store{db: db},
		provider: &httpProvider{
			host:   cfg.ProviderHosts[0],
			token:  cfg.APIToken,
			client: &http.Client{Timeout: cfg.PageTimeout},
		},
	}
	log.Printf("listening on %s", cfg.Addr)
	log.Fatal(http.ListenAndServe(cfg.Addr, srv.Routes()))
}
