# Configuration

The importer is configured entirely through environment variables.

## `GITHUB_TOKEN`

A GitHub personal access token with `repo` scope, used to authenticate
requests to the GitHub REST API.

## `GITHUB_REPO`

The repository to import issues from, in `owner/name` format (for example
`devops-thiago/ThrillhouseBot-test`). Required — the service refuses to
start without it.

## `DATABASE_PATH`

Filesystem path to the SQLite database file the importer reads from and
writes to. Defaults to `issues.db` in the working directory.

## `SYNC_LABELS`

Labels to restrict the import to. If unset, all open issues are imported
regardless of label.
