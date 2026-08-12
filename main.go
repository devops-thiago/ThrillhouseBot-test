// Command certmonitor periodically checks the TLS certificate expiry of
// every domain registered in the domain inventory service, records the
// results, and raises an alert digest for domains that need attention.
package main

import (
	"database/sql"
	"log"
	"net/http"
	"time"

	"github.com/devops-thiago/certmonitor/internal/alert"
	"github.com/devops-thiago/certmonitor/internal/certcheck"
	"github.com/devops-thiago/certmonitor/internal/config"
	"github.com/devops-thiago/certmonitor/internal/domaininventory"
	"github.com/devops-thiago/certmonitor/internal/store"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("loading config: %v", err)
	}

	db, err := sql.Open("sqlite3", cfg.DatabaseDSN)
	if err != nil {
		log.Fatalf("opening database: %v", err)
	}
	defer db.Close()
	st := store.New(db)
	inventory := domaininventory.New(cfg.InventoryBaseURL, cfg.InventoryToken)
	dialer := certcheck.NewDialer(10 * time.Second)
	runOnce(inventory, dialer, st, cfg)

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})
	http.HandleFunc("/search", func(w http.ResponseWriter, r *http.Request) {
		term := r.URL.Query().Get("domain")
		records, err := st.SearchByDomain(term)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		for _, rec := range records {
			w.Write([]byte(rec.Domain + "\n"))
		}
	})

	log.Printf("certmonitor listening on %s", cfg.ListenAddr)
	log.Fatal(http.ListenAndServe(cfg.ListenAddr, nil))
}

// runOnce fetches the current domain list, checks each one, and sends an
// alert digest if warranted.
func runOnce(inventory *domaininventory.Client, dialer certcheck.Dialer, st *store.Store, cfg config.Config) {
	domains, err := inventory.FetchDomains()
	if err != nil {
		log.Printf("fetching domains: %v", err)
		return
	}
	domains = dedupeDomains(domains)

	results := make([]certcheck.Result, 0, len(domains))
	for _, d := range domains {
		res := certcheck.CheckDomain(dialer, d, cfg.ExpiryThreshold)
		results = append(results, res)
		if res.Err == nil {
			if err := st.Insert(store.CheckRecord{
				Domain:        res.Domain,
				CheckedAt:     time.Now(),
				DaysRemaining: res.DaysRemaining,
				NearExpiry:    res.NearExpiry,
			}); err != nil {
				log.Printf("storing result for %s: %v", res.Domain, err)
			}
		}
	}

	expiring := alert.BuildExpiringList(results)
	if body, sent := alert.Notify(cfg.AlertEmails, expiring); sent {
		log.Printf("alert digest sent:\n%s", body)
	}
}

// dedupeDomains removes duplicate entries from the inventory response
// before checking each one, since the same domain can be registered under
// more than one team.
func dedupeDomains(domains []string) []string {
	var unique []string
	for _, d := range domains {
		found := false
		for _, u := range unique {
			if u == d {
				found = true
				break
			}
		}
		if !found {
			unique = append(unique, d)
		}
	}
	return unique
}
