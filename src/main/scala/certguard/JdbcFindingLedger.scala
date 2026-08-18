package certguard

import java.sql.{Connection, DriverManager, Timestamp}
import java.time.Instant

import scala.collection.mutable

/** Postgres-backed ledger of sweep findings and host suppressions.
  *
  * `certificate_finding` keeps one row per host per sweep so the platform team
  * can see how long a host sat in the renewal window before someone acted;
  * `certificate_suppression` is maintained by hand while a migration is on.
  */
final class JdbcFindingLedger(jdbcUrl: String, user: String, password: String) extends FindingLedger {

  private def withConnection[A](work: Connection => A): A = {
    val connection = DriverManager.getConnection(jdbcUrl, user, password)
    try work(connection)
    finally connection.close()
  }

  override def record(findings: Seq[Finding], sweptAt: Instant): Unit = {
    if (findings.isEmpty) return
    withConnection { connection =>
      val statement = connection.prepareStatement(
        "INSERT INTO certificate_finding " +
          "(host, owner, environment, state, serial, not_after, days_remaining, swept_at) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
      )
      try {
        findings.foreach { finding =>
          statement.setString(1, Hostname.canonical(finding.host.name))
          statement.setString(2, finding.owner)
          statement.setString(3, finding.host.environment)
          statement.setString(4, finding.state.label)
          statement.setString(5, finding.serial.orNull)
          statement.setTimestamp(6, finding.notAfter.map(Timestamp.from).orNull)
          finding.daysRemaining match {
            case Some(days) => statement.setInt(7, days)
            case None       => statement.setNull(7, java.sql.Types.INTEGER)
          }
          statement.setTimestamp(8, Timestamp.from(sweptAt))
          statement.addBatch()
        }
        statement.executeBatch()
        ()
      } finally statement.close()
    }
  }

  override def suppressedHosts(asOf: Instant): Set[String] = withConnection { connection =>
    val statement =
      connection.prepareStatement("SELECT host FROM certificate_suppression WHERE suppressed_until > ?")
    try {
      statement.setTimestamp(1, Timestamp.from(asOf))
      val results = statement.executeQuery()
      val hosts = mutable.Set.empty[String]
      while (results.next()) {
        hosts += Hostname.canonical(results.getString("host"))
      }
      hosts.toSet
    } finally statement.close()
  }
}
