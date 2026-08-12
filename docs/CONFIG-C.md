# Configuration

The coupon redemption service is configured entirely through environment
variables, read in `src/main.c`.

## `COUPON_ADMIN_API_URL`

Base URL of the admin API the service syncs its coupon cache from.

- Type: string (URL)
- Default: `http://localhost:8080/coupons`

## `REDEMPTION_LOG_PATH`

Filesystem path to the append-only redemption log written on every
redemption attempt.

- Type: string (file path)
- Default: `./redemptions.log`

## `CACHE_REFRESH_INTERVAL`

How often the in-memory coupon cache is refreshed from the admin API, in
seconds.

- Type: integer (seconds)
- Default: `60`

## `NOTIFY_ALLOWED_CODES`

Coupon code prefixes that should trigger an operator notification when
redeemed.

- Type: string
- Default: none (no codes trigger a notification)
