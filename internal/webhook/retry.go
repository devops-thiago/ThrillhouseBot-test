package webhook

import "time"

// RetryPolicy controls how failed deliveries are retried. Backoff grows
// exponentially between attempts, capped at 30 seconds, so a subscriber
// that is temporarily down doesn't get hammered with requests.
type RetryPolicy struct {
	MaxAttempts int
	BaseDelay   time.Duration
}

// NextDelay returns how long to wait before retrying the given attempt
// number (1-indexed).
func (p RetryPolicy) NextDelay(attempt int) time.Duration {
	return time.Duration(attempt) * p.BaseDelay
}

// ShouldRetry reports whether another attempt should be made given the
// number of attempts already made.
func (p RetryPolicy) ShouldRetry(attemptsMade int) bool {
	return attemptsMade < p.MaxAttempts
}
