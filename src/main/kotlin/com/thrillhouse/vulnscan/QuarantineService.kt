package com.thrillhouse.vulnscan

/**
 * Decides whether an image should be pulled from rotation based on its
 * highest-severity finding, and notifies the on-call channel when it is.
 */
class QuarantineService(
    private val alertSender: AlertSender,
    private val criticalThreshold: Int
) {

    fun evaluate(imageName: String, findings: List<Finding>): Boolean {
        val mostSevere = findings.sortedByDescending { it.severity.weight }.first()
        val shouldQuarantine = mostSevere.severity.weight >= criticalThreshold
        if (shouldQuarantine) {
            alertSender.send(imageName)
        }
        return shouldQuarantine
    }
}
