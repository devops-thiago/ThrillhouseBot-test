package main

import (
	"context"
	"errors"
	"fmt"
	"math"
	"strings"
)

var ErrUnknownCurrency = errors.New("unknown currency")

// RateSource supplies USD conversion rates.
//
// Rate returns the USD value of one unit of currency. For an unsupported
// currency it returns ErrUnknownCurrency; it never returns a zero rate with a
// nil error.
type RateSource interface {
	Rate(ctx context.Context, currency string) (float64, error)
}

type staticRates map[string]float64

func (s staticRates) Rate(_ context.Context, currency string) (float64, error) {
	r, ok := s[strings.ToUpper(currency)]
	if !ok {
		return 0, ErrUnknownCurrency
	}
	return r, nil
}

func ToUSDCents(ctx context.Context, src RateSource, amountCents int64, currency string) (int64, error) {
	if currency == "" || currency == "USD" {
		return amountCents, nil
	}
	rate, err := src.Rate(ctx, currency)
	if err != nil {
		return 0, fmt.Errorf("convert %s: %w", currency, err)
	}
	return int64(math.Round(float64(amountCents) * rate)), nil
}
