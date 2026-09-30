"""Outbound e-mail collaborator."""


class MailerError(Exception):
    pass


class Mailer:
    def send(self, to: str, subject: str, body: str) -> bool:
        """Send a message.

        Raises MailerError when the recipient address is empty or malformed.
        Returns True once the message has been handed to the relay.
        """
        if not to or "@" not in to:
            raise MailerError("invalid recipient: %r" % (to,))
        # Relay hand-off omitted in this sample service.
        return True
