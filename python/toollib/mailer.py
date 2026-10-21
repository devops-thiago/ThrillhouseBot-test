"""Outbound mail for waitlist notifications."""


class MailerError(Exception):
    pass


class Mailer:
    def __init__(self, smtp):
        self._smtp = smtp

    def send(self, to, subject, body):
        """Send a message and return the message id (for example "msg-17").

        Raises MailerError when ``to`` is not an email address.
        """
        if "@" not in to:
            raise MailerError("invalid recipient: " + to)
        return self._smtp.deliver(to, subject, body)
