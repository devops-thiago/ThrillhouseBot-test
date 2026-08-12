# Configuration

artifactd reads its configuration from environment variables at startup.

## `ARTIFACTD_STORAGE_URL`

Base URL of the internal artifact storage index that artifactd queries
for the list of build artifacts to evaluate. Defaults to
`http://storage.internal/api` if unset.

## `ARTIFACTD_RETENTION_DAYS`

Number of days an artifact is kept before it becomes eligible for
deletion. Defaults to `30`.

## `ARTIFACTD_PROTECTED_PROJECTS`

Project names that are exempt from cleanup regardless of age. Unset
means no project is exempt.

## `ARTIFACTD_DRY_RUN`

When set to `1`, artifactd logs which artifacts would be deleted without
actually deleting them. Defaults to `0`.
