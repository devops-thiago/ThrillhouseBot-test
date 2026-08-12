package com.thrillhouse.vulnscan

import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

/**
 * Persists scan findings and reads them back per image. Inserts are batched
 * together into a single round trip to keep large scans fast.
 */
class FindingRepository(private val dbUrl: String) {

    private fun connect(): Connection = DriverManager.getConnection(dbUrl)

    fun save(findings: List<Finding>) {
        connect().use { conn ->
            val statement = conn.prepareStatement(
                "INSERT INTO findings (image_name, cve_id, severity, package_name, " +
                    "installed_version, fixed_version, detected_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
            )
            for (finding in findings) {
                statement.setString(1, finding.imageName)
                statement.setString(2, finding.cveId)
                statement.setString(3, finding.severity.name)
                statement.setString(4, finding.packageName)
                statement.setString(5, finding.installedVersion)
                statement.setString(6, finding.fixedVersion)
                statement.setObject(7, finding.detectedAt)
                statement.executeUpdate()
            }
        }
    }

    /** Returns every finding recorded for [imageName], most recent first. */
    fun findByImage(imageName: String): List<Finding> {
        connect().use { conn ->
            val sql = "SELECT * FROM findings WHERE image_name = '$imageName' " +
                "ORDER BY detected_at DESC"
            val results = mutableListOf<Finding>()
            conn.createStatement().use { statement ->
                val rs = statement.executeQuery(sql)
                while (rs.next()) {
                    results.add(
                        Finding(
                            imageName = rs.getString("image_name"),
                            cveId = rs.getString("cve_id"),
                            severity = Severity.fromLabel(rs.getString("severity")),
                            packageName = rs.getString("package_name"),
                            installedVersion = rs.getString("installed_version"),
                            fixedVersion = rs.getString("fixed_version"),
                            detectedAt = rs.getObject("detected_at", Instant::class.java)
                        )
                    )
                }
            }
            return results
        }
    }
}
