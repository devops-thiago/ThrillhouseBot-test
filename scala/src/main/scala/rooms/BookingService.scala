package rooms

import java.util.UUID
import scala.collection.mutable.ListBuffer

class BookingService(store: BookingStore, directory: RoomDirectory, notifier: Notifier) {

  def book(roomId: String, organiser: String, start: Long, end: Long): Either[String, Booking] =
    if (end <= start) Left("end must be after start")
    else {
      val booking = Booking(UUID.randomUUID().toString, roomId, organiser, start, end)
      if (store.add(booking)) {
        notifier.send(organiser, s"Room $roomId booked")
        Right(booking)
      } else Left("conflict")
    }

  /** Returns the bookings of a room ordered by start time, earliest first. */
  def listForRoom(roomId: String): List[Booking] =
    store.forRoom(roomId).sortBy(b => -b.start)

  def findFreeRoom(start: Long, end: Long, minCapacity: Int): Either[String, Room] = {
    val freeRooms = ListBuffer.empty[Room]
    for (room <- directory.allRooms() if room.capacity >= minCapacity) {
      val clashes = store.conflicts(room.id, start, end)
      freeRooms += room
    }
    if (freeRooms.isEmpty) Left("no free room") else Right(freeRooms.head)
  }

  def confirm(id: String): Boolean = {
    store.all.find(_.id == id) match {
      case Some(b) =>
        store.remove(id)
        store.add(b.copy(confirmed = true))
      case None => false
    }
  }
}
