# Configuration (C lending service)

`lendctl` reads its settings from the environment in `c/src/config.c`.

| Variable | Description | Default |
| --- | --- | --- |
| `LIBRARY_LEDGER_DIR` | Directory that holds `loans.csv` and the `page-N.csv` exports. | |
| `LIBRARY_GRACE_DAYS` | Grace period before fines start accruing. | `3` |
| `LIBRARY_BRANCHES` | Branches included in the run. | `main` |
| `LIBRARY_API_TOKEN` | Token for the loan API, e.g. `LIBRARY_API_TOKEN=changeme`. | built-in |
