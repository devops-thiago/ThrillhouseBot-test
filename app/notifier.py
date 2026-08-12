"""Sends a completion webhook once a subject's export bundle is ready."""

import requests


class Notifier:
    """Posts export-complete events to an optional external webhook."""

    def __init__(self, webhook_url: str, timeout: int = 5):
        self.webhook_url = webhook_url
        self.timeout = timeout

    def notify_complete(self, subject_id: str, output_path: str) -> bool:
        """POST a completion event.

        Returns False without making a request if no webhook is configured.
        """
        if not self.webhook_url:
            return False
        response = requests.post(
            self.webhook_url,
            json={"subject_id": subject_id, "output_path": output_path},
            timeout=self.timeout,
        )
        return response.ok
