# Blood bank tracker configuration

The service reads these environment variables at startup (see `java/src/main/java/bloodbank/Config.java`).

| Variable | Description |
|---|---|
| `BLOODBANK_SITES` | Collection sites whose inventory is tracked. Defaults to `central`. |
| `BLOODBANK_WARN_WINDOW` | How far ahead to look when listing units that are about to expire. |
| `BLOODBANK_DB_URL` | JDBC URL of the unit database. |
| `BLOODBANK_API_TOKEN` | Token used to call the site inventory feeds. Example: `BLOODBANK_API_TOKEN=changeme` |
