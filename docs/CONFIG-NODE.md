# Talk review service settings (node)

| Variable | Description |
|---|---|
| `REVIEW_TRACKS` | Conference tracks the service accepts proposals for, for example `web` or `data`. Empty by default. |
| `REVIEW_CFP_TIMEOUT` | How long to wait for the CFP API before giving up. |
| `REVIEW_PAGE_SIZE` | Number of proposals requested per CFP API page. |
| `REVIEW_API_TOKEN` | Credential for the CFP API. Set `REVIEW_API_TOKEN=changeme` in a local `.env` and replace it. |

Settings are parsed in `node/src/config.js`.
