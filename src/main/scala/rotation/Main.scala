package rotation

import java.util.concurrent.{Executors, TimeUnit}

import scala.concurrent.ExecutionContext

/** Entry point: wires the auditor up from the environment and starts the two
  * background tasks that keep the compliance view current.
  */
object Main {

  private def env(key: String): Option[String] = sys.env.get(key).filter(_.trim.nonEmpty)

  private def required(key: String): String =
    env(key).getOrElse(throw new IllegalStateException(key + " is required"))

  def main(args: Array[String]): Unit = {
    val secretsUrl = required("ROTATION_SECRETS_URL")
    val ticketsUrl = required("ROTATION_TICKETS_URL")
    val apiToken = env("ROTATION_API_TOKEN").getOrElse("")
    val jdbcUrl = env("ROTATION_JDBC_URL")
      .getOrElse("jdbc:postgresql://localhost:5432/compliance")
    val staleAfterDays = env("ROTATION_STALE_AFTER").map(_.trim.toInt).getOrElse(90)
    // The production inventory holds roughly 40,000 credentials, so the
    // listing is requested in large pages to keep the sweep short.
    val pageSize = env("ROTATION_PAGE_SIZE").map(_.trim.toInt).getOrElse(2000)
    val recipients = env("ROTATION_ALERT_RECIPIENTS")
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toSeq)
      .getOrElse(Seq("security@example.com"))

    val workers = Executors.newFixedThreadPool(8)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(workers)

    val auditor = new RotationAuditor(
      new SecretsInventory(new JdkSecretsGateway(secretsUrl, apiToken), pageSize),
      new RotationPolicy(staleAfterDays),
      new ComplianceStore(jdbcUrl),
      new JdkTicketClient(ticketsUrl, apiToken),
      new LoggingAlertSink(recipients)
    )

    val scheduler = Executors.newScheduledThreadPool(2)

    // Full sweep of the inventory.
    scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        auditor.auditBatch()
        ()
      }
    }, 0L, 900L, TimeUnit.SECONDS)

    // Reconciliation of the per-team counters against the compliance store.
    scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = auditor.reconcileOpenTickets()
    }, 60L, 300L, TimeUnit.SECONDS)

    println("secret-rotation-auditor started; sweeping " + secretsUrl + " every 900s")
  }
}
