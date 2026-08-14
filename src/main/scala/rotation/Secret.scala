package rotation

import java.time.Instant

/** One credential tracked by the secrets manager. `lastRotatedAt` is absent
  * for credentials that have never been rotated since they were imported.
  */
final case class Secret(name: String, owner: String, lastRotatedAt: Option[Instant])

/** One page of the paginated inventory listing. */
final case class SecretsPage(items: Seq[Secret], nextPageToken: Option[String])

/** The outcome of examining a single credential during a sweep. */
final case class AuditResult(secret: Secret, status: String)
