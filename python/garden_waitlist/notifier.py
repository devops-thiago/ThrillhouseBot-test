"""Outbound email for plot offers."""

import smtplib
from dataclasses import dataclass
from email.message import EmailMessage
from email.utils import make_msgid


class NotifierError(Exception):
    """Raised when the mail relay cannot accept a message."""


@dataclass(frozen=True)
class Receipt:
    message_id: str
    recipient: str


class EmailNotifier:
    def __init__(self, host: str, port: int = 25,
                 sender: str = "plots@garden.example.org") -> None:
        self.host = host
        self.port = port
        self.sender = sender

    def send(self, to: str, body: str) -> Receipt:
        """Send ``body`` to ``to``.

        Returns a Receipt when the relay accepts the message. A refused or
        undeliverable message raises NotifierError; this never returns a
        falsy value.
        """
        msg = EmailMessage()
        msg["From"] = self.sender
        msg["To"] = to
        msg["Subject"] = "Community garden plot offer"
        msg["Message-ID"] = make_msgid(domain="garden.example.org")
        msg.set_content(body)
        try:
            with smtplib.SMTP(self.host, self.port, timeout=10) as smtp:
                refused = smtp.send_message(msg)
        except (smtplib.SMTPException, OSError) as exc:
            raise NotifierError(f"could not deliver to {to}: {exc}") from exc
        if refused:
            raise NotifierError(f"relay refused {to}: {refused}")
        return Receipt(message_id=msg["Message-ID"], recipient=to)
