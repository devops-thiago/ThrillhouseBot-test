"""Environment-driven configuration for the issue importer."""
import os


class Config:
    """Loads the settings the importer needs from the process environment."""

    def __init__(self):
        self.github_token = os.environ.get("GITHUB_TOKEN", "")
        self.github_repo = os.environ.get("GITHUB_REPO", "")
        self.database_path = os.environ.get("DATABASE_PATH", "issues.db")

        raw_labels = os.environ.get("SYNC_LABELS", "")
        self.sync_labels = [label.strip() for label in raw_labels.split(",") if label.strip()]

    def validate(self):
        """Raise if a required setting is missing."""
        if not self.github_repo:
            raise ValueError("GITHUB_REPO is required (expected 'owner/name')")
        if not self.github_token:
            raise ValueError("GITHUB_TOKEN is required")
