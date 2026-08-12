package flagsync

/** Buckets a client id into the range [0, 100) for rollout percentage
  * comparisons in [[FlagStore.evaluate]].
  *
  * The bucket is a function of `clientId`: the same id always maps to the
  * same bucket, and different ids are spread across the range so a
  * rollout percentage selects a consistent, roughly proportional slice of
  * clients.
  */
trait ClientBucketer {
  def bucketOf(clientId: String): Int
}

/** Default bucketer: a stable hash of the client id, reduced into
  * [0, 100).
  */
object DefaultClientBucketer extends ClientBucketer {
  override def bucketOf(clientId: String): Int = Math.floorMod(clientId.hashCode, 100)
}
