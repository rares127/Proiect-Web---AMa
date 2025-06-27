package ama.database.dao;

import ama.database.DatabaseManager;
import ama.models.Domain;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DomainDAO {
    private DatabaseManager dbManager;

    public DomainDAO() {
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException | java.io.IOException e) {
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
    }

    public List<Domain> getAllDomains() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT id, code, name
                FROM domains
                ORDER BY name ASC
                """;

            List<Domain> domains = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Domain domain = new Domain();
                    domain.setId(rs.getInt("id"));
                    domain.setCode(rs.getString("code"));
                    domain.setName(rs.getString("name"));

                    domains.add(domain);
                }
            }

            return domains;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }
}