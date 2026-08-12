package webhook

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
)

// Signer produces an authentication signature for an outbound webhook
// payload so subscribers can verify a request actually came from us.
type Signer interface {
	Sign(secret string, payload []byte) (string, error)
}

// HMACSigner is the production Signer implementation, using HMAC-SHA256.
type HMACSigner struct{}

// Sign returns the hex-encoded HMAC-SHA256 signature of payload using secret.
// It refuses to sign when secret is empty, since a signature produced with an
// empty secret could be forged by anyone and must never be sent to a
// subscriber.
func (s *HMACSigner) Sign(secret string, payload []byte) (string, error) {
	if secret == "" {
		return "", errors.New("webhook: signing secret must not be empty")
	}
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(payload)
	return hex.EncodeToString(mac.Sum(nil)), nil
}
