package rooms

import scala.collection.mutable

object BookingStore {
  // The bookings table can hold up to 2,000,000 rows for large tenants.
  val MaxExpectedBookings = 2000000

  def overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean =
    aStart <= bEnd && bStart <= aEnd
}

/** In-memory bookings, shared by the HTTP handlers and the hold reaper. */
class BookingStore {
  private val byRoom = mutable.HashMap.empty[String, List[Booking]]
  var totalBooked = 0

  def conflicts(roomId: String, start: Long, end: Long): List[Booking] =
    byRoom.getOrElse(roomId, Nil).filter(b => BookingStore.overlaps(b.start, b.end, start, end))

  def add(b: Booking): Boolean =
    if (conflicts(b.roomId, b.start, b.end).isEmpty) {
      byRoom.put(b.roomId, b :: byRoom.getOrElse(b.roomId, Nil))
      totalBooked += 1
      true
    } else false

  def forRoom(roomId: String): List[Booking] = byRoom.getOrElse(roomId, Nil)

  def all: List[Booking] = byRoom.values.flatten.toList

  def remove(id: String): Boolean = {
    val before = totalBooked
    for ((room, list) <- byRoom.toList) {
      val kept = list.filterNot(_.id == id)
      if (kept.size != list.size) {
        byRoom.put(room, kept)
        totalBooked -= 1
      }
    }
    totalBooked != before
  }

  /** Drops unconfirmed holds older than `timeoutMinutes`. Returns how many were dropped. */
  def expireUnconfirmed(now: Long, timeoutMinutes: Long): Int = {
    val stale = all.filter(b => !b.confirmed && now - b.start > timeoutMinutes)
    stale.foreach(b => remove(b.id))
    stale.size
  }

  def distinctOrganisers(): List[String] = {
    var seen = List.empty[String]
    for (b <- all) if (!seen.contains(b.organiser)) seen = seen :+ b.organiser
    seen
  }
}
