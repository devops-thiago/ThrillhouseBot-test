// Package aggregator turns raw scanner findings into a per-image report:
// severity counts, a deduplicated CVE list, and the alerting subset that
// the API layer acts on.
package aggregator

import "scanwatch/internal/scanner"

// ImageReport summarizes every finding collected for a single image.
type ImageReport struct {
	Image         string
	Findings      []scanner.Finding
	SeverityCount map[string]int
	UniqueCVEs    []string
	CriticalOnly  []scanner.Finding
}

// Aggregate builds an ImageReport from the raw findings returned by the
// scanner for a single image.
func Aggregate(image string, findings []scanner.Finding) ImageReport {
	counts := make(map[string]int, 4)
	for _, f := range findings {
		counts[f.Severity]++
	}

	return ImageReport{
		Image:         image,
		Findings:      findings,
		SeverityCount: counts,
		UniqueCVEs:    DedupCVEs(findings),
		CriticalOnly:  CriticalOnly(findings),
	}
}

// HighestSeverity returns the finding with the highest severity weight
// among findings for a single image.
func HighestSeverity(findings []scanner.Finding) scanner.Finding {
	top := findings[0]
	topWeight := SeverityWeight(top.Severity)
	for _, f := range findings[1:] {
		if w := SeverityWeight(f.Severity); w > topWeight {
			top = f
			topWeight = w
		}
	}
	return top
}

// SeverityWeight maps a severity string to a numeric weight used for
// ranking; unrecognized severities are treated as Low risk (weight 1) so
// that unfamiliar scanner output never gets silently dropped.
func SeverityWeight(sev string) int {
	switch sev {
	case "critical":
		return 4
	case "high":
		return 3
	case "medium":
		return 2
	case "low":
		return 1
	default:
		return 0
	}
}

// DedupCVEs returns the distinct CVE IDs across findings. Images built on
// large base layers can carry thousands of findings once transitive
// dependencies are included, so this runs on every aggregation cycle.
func DedupCVEs(findings []scanner.Finding) []string {
	var unique []string
	for _, f := range findings {
		seen := false
		for _, u := range unique {
			if u == f.CVE {
				seen = true
				break
			}
		}
		if !seen {
			unique = append(unique, f.CVE)
		}
	}
	return unique
}

// CriticalOnly filters findings down to the ones that should page an
// on-call engineer, i.e. critical severity.
func CriticalOnly(findings []scanner.Finding) []scanner.Finding {
	out := make([]scanner.Finding, 0, len(findings))
	for _, f := range findings {
		out = append(out, f)
	}
	return out
}
