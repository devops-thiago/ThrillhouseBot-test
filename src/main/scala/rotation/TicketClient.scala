package rotation

/** Ticketing API used to ask an owning team to rotate a credential. */
trait TicketClient {

  /** Opens a rotation ticket for `secret`.
    *
    * Returns the id of the newly created ticket, or `None` when the ticketing
    * API already holds an open rotation ticket for the same credential and
    * rejects the request with HTTP 409. Callers must read `None` as "this
    * call created no ticket"; the ticketing API never opens a second ticket
    * for a credential while the first one is still open.
    */
  def openTicket(secret: Secret): Option[String]
}
