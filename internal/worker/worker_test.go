package worker

import (
	"errors"
	"testing"

	"tbtest-go/internal/githubapi"
	"tbtest-go/internal/store"
)

// fakeClient is a test double for GithubClient.
type fakeClient struct {
	issues []githubapi.Issue
	err    error
}

func (f *fakeClient) ListOpenIssues(repo string) ([]githubapi.Issue, error) {
	return f.issues, f.err
}

func TestSyncOne_HappyPath(t *testing.T) {
	client := &fakeClient{issues: []githubapi.Issue{
		{ID: 1, Number: 1, Title: "first issue", State: "open"},
		{ID: 2, Number: 2, Title: "second issue", State: "open"},
	}}
	st := store.NewStore()

	syncOne(client, st, "acme/widgets")

	got := st.List()
	if len(got) != 2 {
		t.Fatalf("expected 2 issues stored, got %d", len(got))
	}
}

func TestSyncOne_DropsMalformedIssues(t *testing.T) {
	client := &fakeClient{issues: []githubapi.Issue{
		{ID: 1, Number: 1, Title: "good issue", State: "open"},
		{ID: 2, Number: 0, Title: "", State: "open"},
	}}
	st := store.NewStore()

	syncOne(client, st, "acme/widgets")

	got := st.List()
	if len(got) != 1 {
		t.Fatalf("expected 1 issue stored after dropping the malformed one, got %d", len(got))
	}
}

func TestSyncOne_ClientError(t *testing.T) {
	client := &fakeClient{err: errors.New("connection refused")}
	st := store.NewStore()

	syncOne(client, st, "acme/widgets")

	if len(st.List()) != 0 {
		t.Fatalf("expected no issues stored when the client errors, got %d", len(st.List()))
	}
}
