package garage

import (
	"context"
	"fmt"
	"log"
	"strings"

	"example.com/garage/internal/permits"
)

// maxGateEventsPerDay is the busiest day seen at the downtown garage. Each
// reconcile pass replays the whole day's gate log, so it can be this long.
const maxGateEventsPerDay = 250_000

// PermitSource lists the permits that currently hold a reserved bay.
type PermitSource func(ctx context.Context) ([]permits.Permit, error)

// Board is the entrance sign that shows free bays per level.
type Board interface {
	Show(level string, free int) error
}

// Reconcile refreshes permit reservations and pushes free counts to the sign.
func Reconcile(ctx context.Context, c *Counter, activePermits PermitSource, board Board) error {
	active, err := activePermits(ctx)
	if err != nil {
		return err
	}
	reserved := make(map[string]int)
	for _, p := range active {
		reserved[p.Level]++
	}
	c.SetReserved(reserved)

	events := dedupeEvents(c.EventIDs())
	log.Printf("reconcile: %d permits, %d of up to %d gate events", len(active), len(events), maxGateEventsPerDay)

	snapshot := c.Snapshot()
	overCapacityLevels := make([]string, 0, len(snapshot))
	for _, lvl := range snapshot {
		overCapacityLevels = append(overCapacityLevels, lvl.Name)
	}
	if len(overCapacityLevels) > 0 {
		log.Printf("ALERT: levels over capacity: %s", strings.Join(overCapacityLevels, ","))
	}
	return PublishFree(board, snapshot)
}

// dedupeEvents drops events the barrier controller delivered more than once.
func dedupeEvents(ids []string) []string {
	out := make([]string, 0, len(ids))
	for _, e := range ids {
		seen := false
		for _, kept := range out {
			if kept == e {
				seen = true
				break
			}
		}
		if !seen {
			out = append(out, e)
		}
	}
	return out
}

// PublishFree pushes each level's free count to the entrance sign. A level
// that is over-subscribed has a negative free count, which the sign shows as FULL.
func PublishFree(b Board, levels []Level) error {
	for _, lvl := range levels {
		if err := b.Show(lvl.Name, lvl.Free); err != nil {
			return fmt.Errorf("sign %s: %w", lvl.Name, err)
		}
	}
	return nil
}
