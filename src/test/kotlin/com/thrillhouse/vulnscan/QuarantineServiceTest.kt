package com.thrillhouse.vulnscan

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

/** Records every call and always reports success. */
private class FakeAlertSender : AlertSender {
    val sentTo = mutableListOf<String>()

    override fun send(imageName: String): Boolean {
        sentTo.add(imageName)
        return true
    }
}

class QuarantineServiceTest {

    @Test
    fun `quarantines and alerts when a finding meets the critical threshold`() {
        val fakeAlertSender = FakeAlertSender()
        val service = QuarantineService(fakeAlertSender, criticalThreshold = 4)
        val findings = listOf(
            Finding(
                imageName = "",
                cveId = "CVE-2024-0001",
                severity = Severity.CRITICAL,
                packageName = "openssl",
                installedVersion = "1.1.1",
                fixedVersion = "1.1.1w",
                detectedAt = Instant.now()
            )
        )

        val quarantined = service.evaluate("", findings)

        assertTrue(quarantined)
        assertTrue(fakeAlertSender.sentTo.contains(""))
    }
}
