package webhook

import (
	"encoding/json"
	"fmt"
	"net/http"
)

// Subscriber represents a registered webhook endpoint that wants to receive
// events for the topics it has subscribed to.
type Subscriber struct {
	ID     string   `json:"id"`
	URL    string   `json:"url"`
	Secret string   `json:"secret"`
	Topics []string `json:"topics"`
}

// subscriberPage is a single page of results returned by the directory
// service. The service returns up to 100 subscribers per page and sets
// NextCursor when more results are available.
type subscriberPage struct {
	Subscribers []Subscriber `json:"subscribers"`
	NextCursor  string       `json:"next_cursor"`
}

// SubscriberClient fetches subscribers from the directory service.
type SubscriberClient struct {
	BaseURL string
	HTTP    *http.Client
}

// ListSubscribers returns every subscriber currently registered for
// delivery. The directory service can hold several thousand subscribers
// across large deployments, so results are paginated.
func (c *SubscriberClient) ListSubscribers() ([]Subscriber, error) {
	url := fmt.Sprintf("%s/subscribers", c.BaseURL)
	resp, err := c.HTTP.Get(url)
	if err != nil {
		return nil, fmt.Errorf("fetching subscribers: %w", err)
	}
	defer resp.Body.Close()

	var page subscriberPage
	if err := json.NewDecoder(resp.Body).Decode(&page); err != nil {
		return nil, fmt.Errorf("decoding subscriber page: %w", err)
	}

	return uniqueSubscribers(page.Subscribers), nil
}
