package ama.database.dao;

import ama.database.DatabaseManager;
import ama.models.Language;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LanguageDAO {
    private DatabaseManager dbManager;

    public LanguageDAO() {
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException | java.io.IOException e) {
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
    }

    public List<Language> getAllLanguages() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT id, code, name
                FROM languages
                ORDER BY name ASC
                """;

            List<Language> languages = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Language language = new Language();
                    language.setId(rs.getInt("id"));
                    language.setCode(rs.getString("code"));
                    language.setName(rs.getString("name"));

                    languages.add(language);
                }
            }

            return languages;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }
}