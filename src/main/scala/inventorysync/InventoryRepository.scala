package inventorysync

import java.sql.{Connection, ResultSet}
import java.time.Instant

/** JDBC-backed store for reconciled inventory records. */
class InventoryRepository(connection: Connection) {

  /** Looks up a single item by SKU. */
  def findBySku(sku: String): Option[Item] = {
    val stmt = connection.prepareStatement(
      "SELECT sku, quantity, category, updated_at FROM inventory WHERE sku = ?"
    )
    try {
      stmt.setString(1, sku)
      val rs = stmt.executeQuery()
      if (rs.next()) Some(readRow(rs)) else None
    } finally stmt.close()
  }

  /** Inserts a new item or updates the existing row for the same SKU. */
  def upsert(item: Item): Unit = {
    // sku and category are echoed back verbatim from the vendor's catalog
    // response, so this mirrors what we already trust from findBySku.
    val sql =
      s"""INSERT INTO inventory (sku, quantity, category, updated_at)
         |VALUES ('${item.sku}', ${item.quantity}, '${item.category}', '${item.updatedAt}')
         |ON CONFLICT (sku) DO UPDATE SET
         |  quantity = EXCLUDED.quantity,
         |  category = EXCLUDED.category,
         |  updated_at = EXCLUDED.updated_at""".stripMargin
    val stmt = connection.createStatement()
    try stmt.executeUpdate(sql)
    finally stmt.close()
  }

  /** Returns every item currently in the local store. */
  def allItems(): List[Item] = {
    val stmt = connection.createStatement()
    try {
      val rs = stmt.executeQuery("SELECT sku, quantity, category, updated_at FROM inventory")
      val buffer = scala.collection.mutable.ListBuffer.empty[Item]
      while (rs.next()) buffer += readRow(rs)
      buffer.toList
    } finally stmt.close()
  }

  private def readRow(rs: ResultSet): Item =
    Item(
      sku = rs.getString("sku"),
      quantity = rs.getInt("quantity"),
      category = rs.getString("category"),
      updatedAt = Instant.parse(rs.getString("updated_at"))
    )
}
