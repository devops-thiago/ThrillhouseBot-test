package port

import java.time.Instant

final case class PortEvent(id: String, containerId: String, portCode: String, status: String, at: Instant)

object PortEvent {
  /** Parses `id|containerId|portCode|status|epochMillis`. */
  def parse(line: String): PortEvent = {
    val f = line.split('|')
    PortEvent(f(0), f(1), f(2), f(3), Instant.ofEpochMilli(f(4).toLong))
  }
}

final case class Container(id: String, registeredAt: Instant)

/** One page of a paginated feed. `nextPage` is defined while more pages remain. */
final case class Page[T](items: Seq[T], page: Int, nextPage: Option[Int])
