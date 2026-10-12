package com.clinic.waitlist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Read access to the waitlist table, which the referral intake system writes. */
public class JdbcWaitlistStore {

  private static final Pattern CLINIC_ID = Pattern.compile("[A-Z]{2}-\\d{3}");
  private static final String COLUMNS =
      "id, clinic_id, patient_id, patient_name, phone, appointment_type, priority, requested_at";

  private final String dbUrl;

  public JdbcWaitlistStore(String dbUrl) {
    this.dbUrl = dbUrl;
  }

  public List<WaitlistEntry> findByClinic(String clinicId) {
    String sql = "SELECT " + COLUMNS + " FROM waitlist WHERE clinic_id = ?";
    try (Connection c = DriverManager.getConnection(dbUrl);
        PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, clinicId);
      return readAll(ps.executeQuery());
    } catch (SQLException ex) {
      throw new IllegalStateException("lookup failed for clinic " + clinicId, ex);
    }
  }

  public List<WaitlistEntry> searchByPatientName(String clinicId, String patientName) {
    if (!CLINIC_ID.matcher(clinicId).matches()) {
      throw new IllegalArgumentException("malformed clinic id: " + clinicId);
    }
    String sql = "SELECT " + COLUMNS + " FROM waitlist WHERE clinic_id = '" + clinicId
        + "' AND patient_name ILIKE '%" + patientName + "%' ORDER BY requested_at";
    try (Connection c = DriverManager.getConnection(dbUrl);
        Statement st = c.createStatement()) {
      return readAll(st.executeQuery(sql));
    } catch (SQLException ex) {
      throw new IllegalStateException("search failed for clinic " + clinicId, ex);
    }
  }

  private static List<WaitlistEntry> readAll(ResultSet rs) throws SQLException {
    List<WaitlistEntry> out = new ArrayList<>();
    while (rs.next()) {
      out.add(new WaitlistEntry(rs.getString("id"), rs.getString("clinic_id"),
          rs.getString("patient_id"), rs.getString("patient_name"), rs.getString("phone"),
          rs.getString("appointment_type"), rs.getInt("priority"),
          rs.getTimestamp("requested_at").toInstant()));
    }
    return out;
  }
}
