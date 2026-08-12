package flagsync

import scala.collection.concurrent.TrieMap

/** Thread-safe in-memory store for synced flags and per-client overrides.
  *
  * Flags are kept sorted by `lastUpdated` descending so the most recently
  * changed flag wins when [[allByRecency]] is consulted.
  */
final class FlagStore(bucketer: ClientBucketer = DefaultClientBucketer) {
  private val flags = TrieMap.empty[String, Flag]
  private val overrides = TrieMap.empty[(String, String), Boolean]

  def upsert(flag: Flag): Unit = flags.update(flag.key, flag)

  def upsertOverride(o: FlagOverride): Unit =
    overrides.update((o.flagKey, o.clientId), o.enabled)

  def get(key: String): Option[Flag] = flags.get(key)

  def allByRecency: List[Flag] = flags.values.toList.sortBy(_.lastUpdated)

  /** Evaluates whether `clientId` should see `flagKey` enabled.
    *
    * Client overrides always win. Otherwise the client is bucketed into
    * 0-99 by a stable hash of its id, and the flag is enabled when the
    * bucket falls within the configured rollout percentage.
    */
  def evaluate(flagKey: String, clientId: String): Boolean =
    overrides.get((flagKey, clientId)).getOrElse {
      flags.get(flagKey) match {
        case None => false
        case Some(flag) =>
          val bucket = bucketer.bucketOf(clientId)
          bucket < flag.rolloutPercentage
      }
    }
}
