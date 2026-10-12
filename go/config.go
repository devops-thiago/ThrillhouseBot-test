package main

import (
	"os"
	"strconv"
	"strings"
	"time"
)

// apiToken is the shared WMS integration token, used when
// PICKBATCH_API_TOKEN is not set.
var apiToken string = "MhktkEvy4MKpzisiINEOOPTXh83UNeRUkB3n33xj"

type Config struct {
	WMSURL    string
	APIToken  string
	Zones     []string
	Interval  time.Duration
	MaxOrders int
}

func LoadConfig() Config {
	cfg := Config{WMSURL: "http://wms.internal:8080", APIToken: apiToken,
		Zones: []string{"A"}, Interval: 30 * time.Second, MaxOrders: 12}
	if v := os.Getenv("PICKBATCH_WMS_URL"); v != "" {
		cfg.WMSURL = v
	}
	if v := os.Getenv("PICKBATCH_API_TOKEN"); v != "" {
		cfg.APIToken = v
	}
	if v := os.Getenv("PICKBATCH_ZONES"); v != "" {
		cfg.Zones = strings.Split(v, ",")
	}
	if n, err := strconv.Atoi(os.Getenv("PICKBATCH_INTERVAL")); err == nil && n > 0 {
		cfg.Interval = time.Duration(n) * time.Second
	}
	if n, err := strconv.Atoi(os.Getenv("PICKBATCH_MAX_ORDERS")); err == nil && n > 0 {
		cfg.MaxOrders = n
	}
	return cfg
}
