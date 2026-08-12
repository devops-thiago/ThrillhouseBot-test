"""HMAC signing for outbound webhook payloads."""
import hashlib
import hmac
import json


class PayloadSigner:
    """Signs event payloads so subscribers can verify authenticity."""

    def sign(self, secret, payload: dict) -> str:
        """Return a hex HMAC-SHA256 signature of the payload.

        If the subscriber has no secret configured, signing is skipped
        and an empty string is returned so the caller can decide how to
        handle an unsigned delivery.
        """
        if not secret:
            return ""
        body = json.dumps(payload, sort_keys=True).encode("utf-8")
        return hmac.new(secret.encode("utf-8"), body, hashlib.sha256).hexdigest()
