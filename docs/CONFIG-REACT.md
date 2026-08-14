# Expense approval console — configuration

All settings are read at build time from Vite environment variables, so they
must be present in the shell (or in a `.env` file) when `npm run build` runs.

## `VITE_EXPENSE_API_URL`

Base URL of the expense service the console talks to, without a trailing
slash. Defaults to `http://localhost:8080`.

## `VITE_QUEUE_PAGE_SIZE`

How many pending reports the console asks for when it loads the queue.
Defaults to `50`.

## `VITE_POLICY_CATEGORIES`

Expense categories offered in the queue's category filter. Categories not in
this list are still shown under "All categories". Defaults to an empty list,
which hides the filter's per-category options.

## `VITE_AUTO_APPROVE_LIMIT_CENTS`

Largest report total, in cents, that the bulk-approve action may include.
Defaults to `0`, which leaves nothing eligible for bulk approval.
