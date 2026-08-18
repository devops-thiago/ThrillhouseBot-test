package certguard

import java.util.concurrent.{Executors, TimeUnit}

import org.slf4j.LoggerFactory

/** Entry point: wires the sweep from the environment, exposes the status
  * surface and runs the sweep on a fixed schedule.
  */
object Sentinel {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val settings = Settings.fromEnv(sys.env)
    val http = new HttpJson(settings.apiToken)

    val sweep = new ExpirySweep(
      registry = new ServiceRegistryClient(settings.registryUrl, http),
      feed = new CertificateAuthorityFeed(settings.caUrl, http),
      ledger = new JdbcFindingLedger(settings.jdbcUrl, settings.jdbcUser, settings.jdbcPassword),
      courier = new RelayDigestCourier(settings.relayUrl, http),
      window = settings.window,
      ignoredOwners = settings.ignoredOwners
    )

    val status = new StatusServer(settings.statusPort)
    status.start()

    val scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleWithFixedDelay(
      new Runnable {
        override def run(): Unit =
          try status.publish(sweep.run())
          catch {
            case error: Exception => log.error("sweep failed; retrying on the next tick", error)
          }
      },
      0L,
      settings.sweepMinutes.toLong,
      TimeUnit.MINUTES
    )

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        scheduler.shutdownNow()
        status.stop()
      }
    }))

    log.info(
      s"certguard up on :${settings.statusPort}, sweeping every ${settings.sweepMinutes}m " +
        s"(renew within ${settings.window.renewWithinDays}d, urgent within ${settings.window.urgentWithinDays}d)"
    )
  }
}
