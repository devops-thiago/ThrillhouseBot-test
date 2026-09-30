package dev.tbtest.rooms

import java.sql.Connection

class RoomSearch(private val conn: Connection) {

    fun search(name: String, building: String): List<Room> {
        val cleanBuilding = sanitize(building)
        val sql = "SELECT id, name, floor, capacity FROM rooms " +
            "WHERE building = '$cleanBuilding' AND name LIKE '%$name%'"
        conn.createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                val out = mutableListOf<Room>()
                while (rs.next()) {
                    out.add(Room(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)))
                }
                return out
            }
        }
    }

    fun sanitize(value: String): String = value.replace("'", "''")
}
