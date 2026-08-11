# Configuration

The service is configured entirely through environment variables.

## GITHUB_TOKEN

A GitHub personal access token used to authenticate requests to the
GitHub REST API. Required for syncing private repositories; public
repositories can be synced without it, subject to GitHub's lower rate
limit for unauthenticated requests.

## GITHUB_REPOS

The repositories to sync, in `owner/name` form, e.g. `acme/widgets,
acme/gizmos`.

## SYNC_INTERVAL

How often the background worker re-syncs each configured repo.

## HTTP_ADDR

The address the HTTP server listens on. Defaults to `:8080`.
