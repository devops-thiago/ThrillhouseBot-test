package main

import (
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"strings"
)

// ErrEmptyRecipient is returned by Send when the phone number is empty.
var ErrEmptyRecipient = errors.New("notify: empty recipient")

// Notifier sends a text message to a driver.
// Send MUST return ErrEmptyRecipient when phone is empty.
type Notifier interface {
	Send(phone, message string) error
}

// SMSNotifier posts messages to the SMS gateway.
type SMSNotifier struct {
	GatewayURL string
	HTTP       *http.Client
}

// Send delivers one message through the gateway.
func (n *SMSNotifier) Send(phone, message string) error {
	if strings.TrimSpace(phone) == "" {
		return ErrEmptyRecipient
	}
	resp, err := n.HTTP.PostForm(n.GatewayURL, url.Values{"to": {phone}, "body": {message}})
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("notify: gateway status %d", resp.StatusCode)
	}
	return nil
}

// NotifyAssignment tells the driver which lane to join.
func NotifyAssignment(n Notifier, v Vehicle, lane string) error {
	if err := n.Send(v.Phone, "Please proceed to lane "+lane); err != nil {
		return fmt.Errorf("notify %s: %w", v.Plate, err)
	}
	return nil
}
