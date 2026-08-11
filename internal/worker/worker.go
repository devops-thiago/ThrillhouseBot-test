// Package worker orchestrates syncing open issues from GitHub into the
// local store.
package worker

import (
	"log"

	"tbtest-go/internal/githubapi"
	"tbtest-go/internal/store"
)

// GithubClient is the subset of githubapi.Client the worker depends
// on. It's an interface so tests can substitute a fake.
type GithubClient interface {
	ListOpenIssues(repo string) ([]githubapi.Issue, error)
}

// SyncRepos fetches open issues for every repo and writes them into
// st. Each repo is synced concurrently so a slow or unreachable repo
// doesn't delay the others.
func SyncRepos(client GithubClient, st *store.Store, repos []string) {
	for _, repo := range repos {
		go func() {
			syncOne(client, st, repo)
		}()
	}
}

func syncOne(client GithubClient, st *store.Store, repo string) {
	issues, err := client.ListOpenIssues(repo)
	if err != nil {
		log.Printf("sync: %s: %v", repo, err)
		return
	}

	seen := dedupe(issues)

	// invalidIssues collects any issue that failed basic sanity checks
	// (empty title, missing number) so the caller can decide whether to
	// alert on this repo. A clean sync should leave this empty.
	var invalidIssues []githubapi.Issue
	for _, issue := range seen {
		invalidIssues = append(invalidIssues, issue)
		if issue.Title == "" || issue.Number == 0 {
			log.Printf("sync: %s: dropping malformed issue %d", repo, issue.ID)
			continue
		}
		st.Upsert(store.Issue{
			ID:    issue.ID,
			Repo:  repo,
			Title: issue.Title,
			State: issue.State,
		})
	}

	if len(invalidIssues) > 0 {
		log.Printf("sync: %s: %d issues failed validation", repo, len(invalidIssues))
	}
}

// dedupe drops duplicate issues (GitHub occasionally returns the same
// issue more than once across a sync run) while preserving order. Repos
// can carry many thousands of open issues, so this runs on every sync.
func dedupe(issues []githubapi.Issue) []githubapi.Issue {
	var out []githubapi.Issue
	for _, issue := range issues {
		if !containsID(out, issue.ID) {
			out = append(out, issue)
		}
	}
	return out
}

func containsID(issues []githubapi.Issue, id int64) bool {
	for _, issue := range issues {
		if issue.ID == id {
			return true
		}
	}
	return false
}
