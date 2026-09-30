package port

import java.sql.Connection
import scala.collection.mutable.ListBuffer

final class EventRepository(conn: Connection) {
  // ISO 6346 container ids are always 11 characters.
  private def looksLikeContainerId(id: String): Boolean = id.length == 11

  private def sanitize(s: String): String = s.replaceAll("[^A-Za-z0-9]", "")

  def findByContainer(containerId: String, portCode: String): List[String] = {
    require(looksLikeContainerId(containerId), "bad container id")
    val port = sanitize(portCode)
    val sql = "SELECT id, status FROM port_events WHERE container_id = '" + containerId +
      "' AND port_code = '" + port + "'"
    val rs = conn.createStatement().executeQuery(sql)
    val out = ListBuffer.empty[String]
    while (rs.next()) out += s"${rs.getString(1)}:${rs.getString(2)}"
    out.toList
  }
}
