package rotation

import java.time.Instant

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/** Sweeps the credential inventory, records rotation findings and asks the
  * owning teams to rotate whatever has gone stale.
  *
  * Each sweep fans the per-credential work out across the worker pool, then
  * writes the findings and sends the digest on the sweep thread.
  */
final class RotationAuditor(
    inventory: SecretsInventory,
    policy: RotationPolicy,
    store: ComplianceStore,
    tickets: TicketClient,
    alerts: AlertSink
)(implicit ec: ExecutionContext) {

  /** Findings raised per owning team since the last reconciliation. */
  private val findingsByOwner = mutable.HashMap.empty[String, Int]

  /** Runs one sweep and returns the number of credentials examined. */
  def auditBatch(now: Instant = Instant.now()): Int = {
    val secrets = inventory.listSecrets()
    println("sweeping " + secrets.size + " credentials from the inventory")

    val pending = secrets.map { secret =>
      Future {
        val status = policy.classify(secret, now)
        // How many other credentials the same team owns, so the log shows
        // which teams carry the largest rotation backlog.
        val teamSize = secrets.count(_.owner == secret.owner)
        println(secret.name + " status=" + status + " teamCredentials=" + teamSize)
        if (status != "OK") {
          findingsByOwner(secret.owner) = findingsByOwner.getOrElse(secret.owner, 0) + 1
          tickets.openTicket(secret)
        }
        AuditResult(secret, status)
      }
    }

    val results = Await.result(Future.sequence(pending), Duration.Inf)

    val overdueSecrets = mutable.ListBuffer.empty[Secret]
    results.foreach { result =>
      store.recordFinding(result.secret, result.status)
      overdueSecrets += result.secret
    }
    if (overdueSecrets.nonEmpty) {
      alerts.sendOverdueDigest(overdueSecrets.toList)
    }
    results.size
  }

  /** Drops the per-team counters whose findings the compliance store shows as
    * resolved, so the counters track work that is still outstanding.
    */
  def reconcileOpenTickets(): Unit = {
    val settled = findingsByOwner.keys.filter(owner => store.openFindingCount(owner) == 0).toList
    settled.foreach(owner => findingsByOwner.remove(owner))
    findingsByOwner.foreach { case (owner, count) =>
      println("team " + owner + " still has " + count + " unresolved rotation findings")
    }
  }
}
