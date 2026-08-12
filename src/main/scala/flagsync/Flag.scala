package flagsync

/** A feature flag as synced from the remote config service. */
final case class Flag(
    key: String,
    description: String,
    rolloutPercentage: Int,
    environments: List[String],
    lastUpdated: Long
)

/** A per-client override that forces a flag on or off regardless of its
  * rollout percentage.
  */
final case class FlagOverride(flagKey: String, clientId: String, enabled: Boolean)

/** One page of the remote flag listing. */
final case class FlagPage(flags: List[Flag], hasMore: Boolean, nextCursor: Option[String])
