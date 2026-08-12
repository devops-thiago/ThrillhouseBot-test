# Configuration

The certificate expiry monitor reads its configuration from environment
variables. There are no command-line flags.

## `INVENTORY_API_URL`

Base URL of the internal domain inventory API the monitor queries for
the list of domains to check.

- Default: `http://inventory.internal/api`

## `INVENTORY_PAGE_SIZE`

Number of domains requested per page when listing the inventory.

- Default: `50`

## `EXCLUDED_DOMAINS`

Domains to exclude from certificate checks, for teams that manage
their own rotation and don't want paged on failures.

## `ALERT_THRESHOLD_DAYS`

How many days of remaining certificate lifetime trigger an "expiring
soon" Slack notification instead of a healthy status.

- Default: `30`
