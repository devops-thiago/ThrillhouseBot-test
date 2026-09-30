package rooms

/** Remote room directory. Results are cursor-paginated, 50 rooms per page. */
trait DirectoryApi {
  def fetchRooms(cursor: Option[String]): Page[Room]
}

class RoomDirectory(api: DirectoryApi) {

  /** Returns every room known to the directory. */
  def allRooms(): Seq[Room] = api.fetchRooms(None).items

  def byTag(tag: String): Seq[Room] = allRooms().filter(_.tags.contains(tag))
}
