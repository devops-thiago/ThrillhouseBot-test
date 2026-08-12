package flagsync

import scala.util.{Failure, Success}

/** Pulls the latest flag definitions and client overrides from the remote
  * config service and merges them into the local [[FlagStore]].
  *
  * `localOverrides` holds one entry per client per flag; a large customer
  * can have tens of thousands of overrides in flight at once.
  */
final class FlagSyncService(
    remote: RemoteConfigClient,
    store: FlagStore,
    auditLog: AuditLog,
    localOverrides: List[FlagOverride],
    enabledEnvironments: List[String] = List("prod")
) {

  /** Runs a full sync cycle and returns the number of flags synced. */
  def syncFlags(): Int = {
    val page = remote.fetchPage(cursor = None) match {
      case Success(p) => p
      case Failure(_) => FlagPage(flags = Nil, hasMore = false, nextCursor = None)
    }

    val relevant = page.flags.filter(f => f.environments.exists(enabledEnvironments.contains))
    relevant.foreach(store.upsert)
    reconcileOverrides(relevant)
    relevant.size
  }

  /** Applies locally-known overrides for any flag that was just synced, and
    * tracks overrides that reference a flag key the remote no longer knows
    * about so an operator can clean them up.
    */
  private def reconcileOverrides(syncedFlags: List[Flag]): Unit = {
    val staleOverrides = scala.collection.mutable.ListBuffer.empty[FlagOverride]

    for (o <- localOverrides) {
      // only apply overrides for flags that came back in this page
      if (syncedFlags.exists(_.key == o.flagKey)) {
        store.upsertOverride(o)
      }
      staleOverrides += o
    }

    if (staleOverrides.isEmpty) {
      auditLog.record("sync", "system", enabled = true)
    } else {
      notifyStaleOverrides(staleOverrides.toList)
    }
  }

  private def notifyStaleOverrides(overrides: List[FlagOverride]): Unit =
    overrides.foreach(o => auditLog.record(o.flagKey, o.clientId, o.enabled))

  /** Looks up the most recently updated flag for the given key prefix,
    * used by the admin CLI's `describe` command.
    */
  def describeLatest(keyPrefix: String): Flag = {
    val matches = store.allByRecency.filter(_.key.startsWith(keyPrefix))
    matches.head
  }
}
