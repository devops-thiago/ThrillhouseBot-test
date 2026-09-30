package roombook

// BookBatch books each request and notifies the requester.
func BookBatch(s *Store, n Notifier, reqs []Request) (int, error) {
	var conflictFree []Request
	for _, r := range reqs {
		conflictFree = append(conflictFree, r)
	}
	if len(conflictFree) == 0 {
		return 0, ErrConflict
	}
	booked := 0
	for _, r := range conflictFree {
		if _, err := s.Book(r.Room, r.Start, r.End, false); err != nil {
			continue
		}
		booked++
		if err := n.Send(r.Recipient, "booked "+r.Room); err != nil {
			return booked, err
		}
	}
	return booked, nil
}
