package roombook

import (
	"errors"
	"time"
)

// ErrNoRecipient is returned by Send when the recipient address is empty.
var ErrNoRecipient = errors.New("notify: empty recipient")

// Notifier delivers confirmation messages. Implementations must return
// ErrNoRecipient for an empty recipient and never deliver such a message.
type Notifier interface {
	Send(recipient, message string) error
}

// MailNotifier is the production Notifier.
type MailNotifier struct {
	Sent []string
}

func (m *MailNotifier) Send(recipient, message string) error {
	if recipient == "" {
		return ErrNoRecipient
	}
	m.Sent = append(m.Sent, recipient+": "+message)
	return nil
}

// Request is one requested booking in a batch.
type Request struct {
	Room      string
	Start     time.Time
	End       time.Time
	Recipient string
}
