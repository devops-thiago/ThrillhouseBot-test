package com.thrillhouse.vulnscan

/**
 * Aggregates raw findings into per-severity counts, after removing
 * duplicate reports of the same CVE for the same image (the scanner
 * occasionally reports the same vulnerability once per affected layer).
 */
class SeverityAggregator {

    fun summarize(findings: List<Finding>): Map<Severity, Int> {
        val deduped = dedupe(findings)
        val counts = mutableMapOf<Severity, Int>()
        for (finding in deduped) {
            counts[finding.severity] = (counts[finding.severity] ?: 0) + 1
        }
        return counts
    }

    /**
     * Drops repeat reports of the same CVE for the same image. Base images
     * with many layers can produce thousands of findings, most of them
     * duplicates of a handful of underlying CVEs.
     */
    private fun dedupe(findings: List<Finding>): List<Finding> {
        val unique = mutableListOf<Finding>()
        for (finding in findings) {
            val alreadySeen = unique.any {
                it.imageName == finding.imageName && it.cveId == finding.cveId
            }
            if (!alreadySeen) {
                unique.add(finding)
            }
        }
        return unique
    }
}
