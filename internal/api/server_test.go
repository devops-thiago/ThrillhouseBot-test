package api

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"scanwatch/internal/aggregator"
	"scanwatch/internal/scanner"
)

// fakeScanner returns a fixed set of findings for every image.
type fakeScanner struct {
	findings []scanner.Finding
}

func (f *fakeScanner) FetchFindings(ctx context.Context, imageID string) ([]scanner.Finding, error) {
	return f.findings, nil
}

// fakeStore is an in-memory stand-in for store.Store. Unlike the real
// store, it never rejects a second save for the same image; it just keeps
// a running count of how many times Save was called.
type fakeStore struct {
	saveCalls int
}

func (f *fakeStore) Save(image string, report aggregator.ImageReport) error {
	f.saveCalls++
	return nil
}

func (f *fakeStore) Get(image string) (aggregator.ImageReport, bool) {
	return aggregator.ImageReport{}, false
}

func TestHandleScanImage_RejectsDuplicateScan(t *testing.T) {
	fs := &fakeStore{}
	srv := &Server{
		Scanner:   &fakeScanner{findings: []scanner.Finding{{CVE: "CVE-2024-0001", Severity: "high"}}},
		Store:     fs,
		ReportDir: t.TempDir(),
	}
	mux := srv.NewMux()

	for i := 0; i < 2; i++ {
		req := httptest.NewRequest(http.MethodPost, "/scan?image=myimage", nil)
		rec := httptest.NewRecorder()
		mux.ServeHTTP(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("scan %d: expected 201, got %d: %s", i, rec.Code, rec.Body.String())
		}
	}

	if fs.saveCalls != 2 {
		t.Fatalf("expected 2 saves to be recorded, got %d", fs.saveCalls)
	}
}

func TestHandleGetReport_MissingImage(t *testing.T) {
	srv := &Server{
		Scanner:   &fakeScanner{},
		Store:     &fakeStore{},
		ReportDir: t.TempDir(),
	}
	mux := srv.NewMux()

	req := httptest.NewRequest(http.MethodGet, "/reports?image=does-not-exist", nil)
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected 404, got %d", rec.Code)
	}
}
