# Configuration

`branch-janitor` is configured entirely through environment variables.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `GITHUB_TOKEN` | yes | — | Personal access token with `repo` scope, used to authenticate all GitHub API requests. |
| `GITHUB_REPO` | yes | — | The repository to scan, in `owner/name` form. |
| `STALE_DAYS` | no | `90` | Number of days since the last commit before a branch is considered stale and eligible for cleanup. |
| `EXCLUDE_BRANCHES` | no | none | Branch names to exclude from cleanup, in addition to the repository's default branch. |

Deletions are archived locally as `archive/<branch>` git tags before the remote branch is removed, so
the commit history stays reachable for later review.
