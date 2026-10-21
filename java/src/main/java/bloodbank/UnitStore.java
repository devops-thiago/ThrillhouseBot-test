package bloodbank;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reads blood units from the relational store. */
public class UnitStore {

    private static final Pattern BLOOD_TYPE = Pattern.compile("^(A|B|AB|O)[+-]$");

    private final Connection connection;

    public UnitStore(Connection connection) {
        this.connection = connection;
    }

    /** Escapes quotes so a value is safe to embed in a SQL literal. */
    static String sanitize(String value) {
        return value.replace("'", "''");
    }

    /**
     * Lists units of one blood type at a site. The blood type is validated for shape;
     * the site name comes straight from the request query string.
     */
    public List<BloodUnit> findByTypeAndSite(String bloodType, String site) throws SQLException {
        if (!BLOOD_TYPE.matcher(bloodType).matches()) {
            throw new IllegalArgumentException("bad blood type: " + bloodType);
        }
        String safeType = sanitize(bloodType);
        String sql = "SELECT id, site, blood_type, product, collected_at, expires_at FROM units"
                + " WHERE blood_type = '" + safeType + "' AND site = '" + site + "'";
        List<BloodUnit> out = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new BloodUnit(rs.getString("id"), rs.getString("site"),
                        rs.getString("blood_type"), rs.getString("product"),
                        Instant.parse(rs.getString("collected_at")),
                        Instant.parse(rs.getString("expires_at"))));
            }
        }
        return out;
    }
}
