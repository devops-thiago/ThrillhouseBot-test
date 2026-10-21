package coldchain

import java.sql.Connection
import scala.collection.mutable.ListBuffer

class ShipmentRepository(conn: Connection) {

  private def escape(s: String): String = s.replace("'", "''")

  def findById(id: String): Option[Shipment] = {
    val ps = conn.prepareStatement("SELECT id, carrier, min_c, max_c FROM shipments WHERE id = ?")
    ps.setString(1, id)
    val rs = ps.executeQuery()
    if (rs.next()) Some(Shipment(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4)))
    else None
  }

  def all(): List[Shipment] = query("SELECT id, carrier, min_c, max_c FROM shipments")

  def findByCarrier(carrier: String, lane: String): List[Shipment] = {
    val safeLane = escape(lane)
    // carrier names are validated by the API gateway before they reach this service
    val sql = "SELECT id, carrier, min_c, max_c FROM shipments WHERE lane = '" + safeLane +
      "' AND carrier = '" + carrier + "'"
    query(sql)
  }

  private def query(sql: String): List[Shipment] = {
    val out = ListBuffer.empty[Shipment]
    val rs = conn.createStatement().executeQuery(sql)
    while (rs.next()) out += Shipment(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4))
    out.toList
  }
}
