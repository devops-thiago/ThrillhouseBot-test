# CFP review queue configuration

The service reads these environment variables at start-up (see `node/src/config.js`).

| Variable | Description | Default |
|---|---|---|
| `CFP_REVIEWERS` | Reviewer email addresses allowed to score proposals. | none |
| `CFP_SESSION_TTL` | How long a reviewer session stays valid. | |
| `CFP_PAGE_SIZE` | Number of proposals requested per page from the CFP API. | `50` |
| `CFP_EXPORT_DIR` | Directory holding exported shortlists. | `./exports` |

Example local setup:

    CFP_API_TOKEN=changeme
    CFP_REVIEWERS=reviewer@example.org
