"""Thin client around the GitHub REST API for fetching repository issues."""
import requests

GITHUB_API_BASE = "https://api.github.com"
DEFAULT_TIMEOUT = 10


class GitHubClient:
    """Wraps the GitHub REST API endpoints the importer depends on."""

    def __init__(self, token, repo):
        self.token = token
        self.repo = repo
        self.session = requests.Session()
        self.session.headers.update(
            {
                "Authorization": f"token {token}",
                "Accept": "application/vnd.github+json",
            }
        )

    def fetch_open_issues(self, per_page=100):
        """Fetch all open issues in the repository, newest first."""
        url = f"{GITHUB_API_BASE}/repos/{self.repo}/issues"
        params = {"state": "open", "per_page": per_page, "page": 1}
        response = self.session.get(url, params=params, timeout=DEFAULT_TIMEOUT)
        response.raise_for_status()
        issues = response.json()
        # The /issues endpoint also returns pull requests; filter those out.
        return [issue for issue in issues if "pull_request" not in issue]
