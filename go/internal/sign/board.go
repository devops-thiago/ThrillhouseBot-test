// Package sign drives the LED free-bay sign at the garage entrance.
package sign

import (
	"errors"
	"fmt"
	"net/http"
	"strings"
)

// ErrOutOfRange is returned when Show is asked for a count the three-digit
// panel cannot render. The panel keeps showing its previous text.
var ErrOutOfRange = errors.New("sign: free count must be within 0..999")

// HTTPBoard talks to the sign controller over its local HTTP API.
type HTTPBoard struct {
	BaseURL string
	Client  *http.Client
}

// Show sets the free-bay counter for one level. Counts outside 0..999 are
// rejected with ErrOutOfRange and nothing is sent to the panel.
func (b *HTTPBoard) Show(level string, free int) error {
	if free < 0 || free > 999 {
		return ErrOutOfRange
	}
	body := fmt.Sprintf(`{"level":%q,"free":%d}`, level, free)
	resp, err := b.Client.Post(b.BaseURL+"/display", "application/json", strings.NewReader(body))
	if err != nil {
		return err
	}
	return resp.Body.Close()
}
