# Invoicer configuration

Settings are read from environment variables by `Config.fromEnv` in `kotlin/src/main/kotlin/school/invoicer/Config.kt`.

| Variable | Description |
|---|---|
| `INVOICER_DB_URL` | JDBC URL of the enrolment database. Default `jdbc:postgresql://localhost/school`. |
| `INVOICER_CURRENCIES` | Currencies the school invoices in, for example `EUR`. Default `EUR`. |
| `INVOICER_OVERDUE_GRACE` | How long after issue an invoice may stay unpaid before it is flagged overdue. Default 14. |
| `INVOICER_PAGE_SIZE` | Number of courses requested per catalogue page. |

The registry token is passed separately:

    INVOICER_REGISTRY_TOKEN=changeme
