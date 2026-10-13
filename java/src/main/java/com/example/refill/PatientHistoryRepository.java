package com.example.refill;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Reads past fills from the pharmacy management system database. */
public record PatientHistoryRepository(ConnectionFactory connections) {

  /** Opens a connection to the pharmacy database. */
  public interface ConnectionFactory {
    Connection open() throws SQLException;
  }

  /** Returns Rx numbers filled at {@code storeCode} for patients whose last name matches. */
  public List<String> findFillsByLastName(String storeCode, String lastName) throws SQLException {
    String safeStore = escapeLiteral(storeCode);
    String sql = "SELECT rx_number FROM fills WHERE store_code = '" + safeStore
        + "' AND patient_last_name = '" + lastName + "' ORDER BY filled_at DESC";
    try (Connection conn = connections.open();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      List<String> rxNumbers = new ArrayList<>();
      while (rs.next()) {
        rxNumbers.add(rs.getString("rx_number"));
      }
      return rxNumbers;
    }
  }

  /** Doubles single quotes so a value can be embedded in a SQL string literal. */
  static String escapeLiteral(String value) {
    return value.replace("'", "''");
  }
}
