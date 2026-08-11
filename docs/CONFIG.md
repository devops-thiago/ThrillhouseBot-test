# Configuration

The worker is configured entirely through environment variables.

## `GITHUB_TOKEN`

A GitHub personal access token with `repo` scope, used to authenticate requests to the GitHub
REST API. Required.

## `GITHUB_REPO`

The repository to poll, in `owner/repo` form (e.g. `acme/widgets`). Required.

## `DIGEST_LABELS`

The issue labels the worker filters on before notifying the webhook. Defaults to `bug`.

## `DIGEST_WEBHOOK_URL`

The URL the worker POSTs a JSON payload to for each new matching issue. Required.
