package ama.database.dao;

import ama.database.DatabaseManager;
import ama.models.Abbreviation;
import ama.models.Language;
import ama.models.Domain;
import ama.models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AbbreviationDAO {
    private DatabaseManager dbManager;

    public AbbreviationDAO() {
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException | java.io.IOException e) {
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
    }

    public Abbreviation create(Abbreviation abbreviation) throws SQLException {
        if (abbreviation == null || abbreviation.getName() == null || abbreviation.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Abbreviation data is incomplete");
        }

        // verificam daca exista in combinatia de limba si domeniu
        if (abbreviationExists(abbreviation.getName(), abbreviation.getLanguageId(),
                abbreviation.getDomainId(), null)) {
            throw new SQLException("Abrevierea există deja în această combinație de limbă și domeniu");
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
            INSERT INTO abbreviations (name, language_id, domain_id, user_id, description, docbook) 
            VALUES (?, ?, ?, ?, ?, ?) 
            RETURNING id, created_at
            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, abbreviation.getName().trim());
                stmt.setInt(2, abbreviation.getLanguageId());
                stmt.setInt(3, abbreviation.getDomainId());
                stmt.setInt(4, abbreviation.getUserId());
                stmt.setString(5, abbreviation.getDescription());
                stmt.setString(6, abbreviation.getDocbook());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        abbreviation.setId(rs.getInt("id"));
                        abbreviation.setCreatedAt(rs.getTimestamp("created_at"));
                        return abbreviation;
                    }
                    throw new SQLException("Failed to create abbreviation");
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean abbreviationExists(String name, int languageId, int domainId, Integer excludeId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) FROM abbreviations WHERE UPPER(name) = UPPER(?) AND language_id = ? AND domain_id = ?"
            );

            if (excludeId != null) {
                sql.append(" AND id != ?");
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                stmt.setString(1, name.trim());
                stmt.setInt(2, languageId);
                stmt.setInt(3, domainId);

                if (excludeId != null) {
                    stmt.setInt(4, excludeId);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public void addMeanings(int abbreviationId, List<String> meanings) throws SQLException {
        if (meanings == null || meanings.isEmpty()) {
            return;
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = "INSERT INTO abbreviation_meanings (abbreviation_id, meaning, sort_order) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < meanings.size(); i++) {
                    stmt.setInt(1, abbreviationId);
                    stmt.setString(2, meanings.get(i).trim()); // Trim pt safety
                    stmt.setInt(3, i); /// sort_order incepe de la 0
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public Optional<Abbreviation> findById(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT a.id, a.name, a.language_id, a.domain_id, a.user_id, 
                       a.description, a.docbook, a.views, a.likes, a.favorites, a.created_at,
                       l.code as lang_code, l.name as lang_name,
                       d.code as domain_code, d.name as domain_name,
                       u.username, u.email
                FROM abbreviations a
                JOIN languages l ON a.language_id = l.id
                JOIN domains d ON a.domain_id = d.id
                JOIN users u ON a.user_id = u.id
                WHERE a.id = ?
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Abbreviation abbr = mapResultSetToAbbreviation(rs);
                        abbr.setMeanings(getMeaningsForAbbreviation(conn, id));
                        return Optional.of(abbr);
                    }
                    return Optional.empty();
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public List<Abbreviation> search(String searchTerm, String languageCode, String domainCode) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.name, a.description, a.views, a.likes, a.favorites, a.created_at,
                       a.language_id, a.domain_id, a.user_id, a.docbook,
                       l.code as lang_code, l.name as lang_name,
                       d.code as domain_code, d.name as domain_name,
                       u.username
                FROM abbreviations a
                JOIN languages l ON a.language_id = l.id
                JOIN domains d ON a.domain_id = d.id
                JOIN users u ON a.user_id = u.id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql.append(" AND (a.name ILIKE ? OR a.description ILIKE ?)");
                String searchPattern = "%" + searchTerm.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            if (languageCode != null && !languageCode.trim().isEmpty()) {
                sql.append(" AND l.code = ?");
                params.add(languageCode.trim());
            }

            if (domainCode != null && !domainCode.trim().isEmpty()) {
                sql.append(" AND d.code = ?");
                params.add(domainCode.trim());
            }

            sql.append(" ORDER BY a.name");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    List<Abbreviation> results = new ArrayList<>();
                    while (rs.next()) {
                        try {
                            Abbreviation abbr = mapResultSetToAbbreviation(rs);
                            // se add semnifcatia pt fiecare abrev
                            abbr.setMeanings(getMeaningsForAbbreviation(conn, abbr.getId()));
                            results.add(abbr);
                        } catch (SQLException e) {
                            System.err.println("Error mapping abbreviation with ID " + rs.getInt("id") + ": " + e.getMessage());
                        }
                    }
                    return results;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public List<Abbreviation> getMostPopular(int limit) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT a.id, a.name, a.description, a.views, a.likes, a.favorites, a.created_at,
                       a.language_id, a.domain_id, a.user_id, a.docbook,
                       l.code as lang_code, l.name as lang_name,
                       d.code as domain_code, d.name as domain_name,
                       u.username
                FROM abbreviations a
                JOIN languages l ON a.language_id = l.id
                JOIN domains d ON a.domain_id = d.id
                JOIN users u ON a.user_id = u.id
                ORDER BY a.views DESC, a.likes DESC
                LIMIT ?
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Abbreviation> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapResultSetToAbbreviation(rs));
                    }
                    return results;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public void incrementViews(int abbreviationId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "UPDATE abbreviations SET views = views + 1 WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, abbreviationId);
                stmt.executeUpdate();
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean toggleLike(int abbreviationId, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            ///  verificam daca am dat like deja
            String checkSql = "SELECT 1 FROM abbreviation_likes WHERE abbreviation_id = ? AND user_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, abbreviationId);
                checkStmt.setInt(2, userId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // daca exista - il sterg
                        String deleteSql = "DELETE FROM abbreviation_likes WHERE abbreviation_id = ? AND user_id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, abbreviationId);
                            deleteStmt.setInt(2, userId);
                            deleteStmt.executeUpdate();
                            return false; // sters
                        }
                    } else {
                        // Nu exista -> add like
                        String insertSql = "INSERT INTO abbreviation_likes (abbreviation_id, user_id) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, abbreviationId);
                            insertStmt.setInt(2, userId);
                            insertStmt.executeUpdate();
                            return true; // add
                        }
                    }
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean toggleFavorite(int abbreviationId, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String checkSql = "SELECT 1 FROM abbreviation_favorites WHERE abbreviation_id = ? AND user_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, abbreviationId);
                checkStmt.setInt(2, userId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        /// e deja fav - șterg fav
                        String deleteSql = "DELETE FROM abbreviation_favorites WHERE abbreviation_id = ? AND user_id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, abbreviationId);
                            deleteStmt.setInt(2, userId);
                            deleteStmt.executeUpdate();
                            return false; // sters
                        }
                    } else {
                        ///  nu e la favorite, il add
                        String insertSql = "INSERT INTO abbreviation_favorites (abbreviation_id, user_id) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, abbreviationId);
                            insertStmt.setInt(2, userId);
                            insertStmt.executeUpdate();
                            return true; // add
                        }
                    }
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean userHasLiked(int abbreviationId, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "SELECT 1 FROM abbreviation_likes WHERE abbreviation_id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, abbreviationId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean userHasFavorited(int abbreviationId, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "SELECT 1 FROM abbreviation_favorites WHERE abbreviation_id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, abbreviationId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public List<Abbreviation> getUserAbbreviations(int userId, int limit, int offset) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
            SELECT a.id, a.name, a.description, a.views, a.likes, a.favorites, a.created_at,
                   a.language_id, a.domain_id, a.user_id, a.docbook,
                   l.code as lang_code, l.name as lang_name,
                   d.code as domain_code, d.name as domain_name,
                   u.username
            FROM abbreviations a
            LEFT JOIN languages l ON a.language_id = l.id
            LEFT JOIN domains d ON a.domain_id = d.id
            LEFT JOIN users u ON a.user_id = u.id
            WHERE a.user_id = ?
            ORDER BY a.created_at DESC
            LIMIT ? OFFSET ?
            """;

            List<Abbreviation> abbreviations = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Abbreviation abbrev = mapResultSetToAbbreviation(rs);
                        abbrev.setMeanings(getMeaningsForAbbreviation(conn, abbrev.getId()));
                        abbreviations.add(abbrev);
                    }
                }
            }

            return abbreviations;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public List<Abbreviation> getAll(int limit, int offset) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
            SELECT a.id, a.name, a.description, a.views, a.likes, a.favorites, a.created_at,
                   a.language_id, a.domain_id, a.user_id, a.docbook,
                   l.code as lang_code, l.name as lang_name,
                   d.code as domain_code, d.name as domain_name,
                   u.username
            FROM abbreviations a
            LEFT JOIN languages l ON a.language_id = l.id
            LEFT JOIN domains d ON a.domain_id = d.id
            LEFT JOIN users u ON a.user_id = u.id
            ORDER BY a.created_at DESC
            LIMIT ? OFFSET ?
            """;

            List<Abbreviation> abbreviations = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Abbreviation abbrev = mapResultSetToAbbreviation(rs);
                        abbrev.setMeanings(getMeaningsForAbbreviation(conn, abbrev.getId()));
                        abbreviations.add(abbrev);
                    }
                }
            }

            return abbreviations;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public void update(Abbreviation abbreviation) throws SQLException {
        ///  verificam ca inainte de update sa nu se repete combinatia de limba si domeniu
        if (abbreviationExists(abbreviation.getName(), abbreviation.getLanguageId(),
                abbreviation.getDomainId(), abbreviation.getId())) {
            throw new SQLException("Abrevierea există deja în această combinație de limbă și domeniu");
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
        UPDATE abbreviations 
        SET name = ?, description = ?, language_id = ?, domain_id = ?, docbook = ?
        WHERE id = ?
        """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, abbreviation.getName().trim());
                stmt.setString(2, abbreviation.getDescription());
                stmt.setInt(3, abbreviation.getLanguageId());
                stmt.setInt(4, abbreviation.getDomainId());
                stmt.setString(5, abbreviation.getDocbook());
                stmt.setInt(6, abbreviation.getId());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Nu s-a putut actualiza abrevierea - niciun rând afectat");
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean delete(int abbreviationId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            // stergem tot ce e legate de abreviere
            try {
                clearMeanings(conn, abbreviationId);

                String deleteLikesSQL = "DELETE FROM abbreviation_likes WHERE abbreviation_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteLikesSQL)) {
                    stmt.setInt(1, abbreviationId);
                    stmt.executeUpdate();
                }

                String deleteFavoritesSQL = "DELETE FROM abbreviation_favorites WHERE abbreviation_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteFavoritesSQL)) {
                    stmt.setInt(1, abbreviationId);
                    stmt.executeUpdate();
                }

                String deleteAbbreviationSQL = "DELETE FROM abbreviations WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteAbbreviationSQL)) {
                    stmt.setInt(1, abbreviationId);
                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        conn.commit();
                        return true;
                    } else {
                        conn.rollback();
                        return false;
                    }
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public void clearMeanings(int abbreviationId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = "DELETE FROM abbreviation_meanings WHERE abbreviation_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, abbreviationId);
                stmt.executeUpdate();
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /// pt conexiune existenta - helper
    private void clearMeanings(Connection conn, int abbreviationId) throws SQLException {
        String sql = "DELETE FROM abbreviation_meanings WHERE abbreviation_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, abbreviationId);
            stmt.executeUpdate();
        }
    }

    public List<Abbreviation> getUserFavorites(int userId, int limit, int offset) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT a.id, a.name, a.description, a.views, a.likes, a.favorites, a.created_at,
                       a.language_id, a.domain_id, a.user_id, a.docbook,
                       l.code as lang_code, l.name as lang_name,
                       d.code as domain_code, d.name as domain_name,
                       u.username
                FROM abbreviations a
                JOIN abbreviation_favorites af ON a.id = af.abbreviation_id
                JOIN languages l ON a.language_id = l.id
                JOIN domains d ON a.domain_id = d.id
                JOIN users u ON a.user_id = u.id
                WHERE af.user_id = ?
                ORDER BY af.created_at DESC
                LIMIT ? OFFSET ?
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Abbreviation> results = new ArrayList<>();
                    while (rs.next()) {
                        try {
                            Abbreviation abbr = mapResultSetToAbbreviation(rs);
                            abbr.setMeanings(getMeaningsForAbbreviation(conn, abbr.getId()));
                            results.add(abbr);
                        } catch (SQLException e) {
                            System.err.println("Error mapping abbreviation with ID " + rs.getInt("id") + ": " + e.getMessage());
                        }
                    }
                    return results;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public int getTotalCount() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "SELECT COUNT(*) FROM abbreviations";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public java.util.List<ama.servlets.StatisticsServlet.LanguageStatistic> getStatisticsByLanguage() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT l.name, l.code, COUNT(a.id) as count,
                       ROUND(COUNT(a.id) * 100.0 / (SELECT COUNT(*) FROM abbreviations), 1) as percentage
                FROM languages l
                LEFT JOIN abbreviations a ON l.id = a.language_id
                GROUP BY l.id, l.name, l.code
                HAVING COUNT(a.id) > 0
                ORDER BY count DESC
                """;

            java.util.List<ama.servlets.StatisticsServlet.LanguageStatistic> stats = new java.util.ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ama.servlets.StatisticsServlet.LanguageStatistic stat = new ama.servlets.StatisticsServlet.LanguageStatistic();
                    stat.languageName = rs.getString("name");
                    stat.languageCode = rs.getString("code");
                    stat.count = rs.getInt("count");
                    stat.percentage = rs.getDouble("percentage");
                    stats.add(stat);
                }
            }

            return stats;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public java.util.List<ama.servlets.StatisticsServlet.DomainStatistic> getStatisticsByDomain() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT d.name, d.code, COUNT(a.id) as count,
                       ROUND(COUNT(a.id) * 100.0 / (SELECT COUNT(*) FROM abbreviations), 1) as percentage
                FROM domains d
                LEFT JOIN abbreviations a ON d.id = a.domain_id
                GROUP BY d.id, d.name, d.code
                HAVING COUNT(a.id) > 0
                ORDER BY count DESC
                """;

            java.util.List<ama.servlets.StatisticsServlet.DomainStatistic> stats = new java.util.ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ama.servlets.StatisticsServlet.DomainStatistic stat = new ama.servlets.StatisticsServlet.DomainStatistic();
                    stat.domainName = rs.getString("name");
                    stat.domainCode = rs.getString("code");
                    stat.count = rs.getInt("count");
                    stat.percentage = rs.getDouble("percentage");
                    stats.add(stat);
                }
            }

            return stats;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public java.util.List<ama.servlets.StatisticsServlet.AbbreviationStatistic> getTopByViews(int limit) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT a.name, l.name as language, d.name as domain, u.username as author,
                       a.views, a.likes, a.created_at
                FROM abbreviations a
                JOIN languages l ON a.language_id = l.id
                JOIN domains d ON a.domain_id = d.id
                JOIN users u ON a.user_id = u.id
                ORDER BY a.views DESC, a.likes DESC
                LIMIT ?
                """;

            java.util.List<ama.servlets.StatisticsServlet.AbbreviationStatistic> stats = new java.util.ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ama.servlets.StatisticsServlet.AbbreviationStatistic stat = new ama.servlets.StatisticsServlet.AbbreviationStatistic();
                        stat.name = rs.getString("name");
                        stat.language = rs.getString("language");
                        stat.domain = rs.getString("domain");
                        stat.author = rs.getString("author");
                        stat.views = rs.getInt("views");
                        stat.likes = rs.getInt("likes");
                        stat.createdAt = formatDate(rs.getTimestamp("created_at"));
                        stats.add(stat);
                    }
                }
            }

            return stats;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public java.util.List<ama.servlets.StatisticsServlet.AbbreviationStatistic> getTopByLikes(int limit) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT a.name, l.name as language, d.name as domain, u.username as author,
                       a.views, a.likes, a.created_at
                FROM abbreviations a
                JOIN languages l ON a.language_id = l.id
                JOIN domains d ON a.domain_id = d.id
                JOIN users u ON a.user_id = u.id
                ORDER BY a.likes DESC, a.views DESC
                LIMIT ?
                """;

            java.util.List<ama.servlets.StatisticsServlet.AbbreviationStatistic> stats = new java.util.ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ama.servlets.StatisticsServlet.AbbreviationStatistic stat = new ama.servlets.StatisticsServlet.AbbreviationStatistic();
                        stat.name = rs.getString("name");
                        stat.language = rs.getString("language");
                        stat.domain = rs.getString("domain");
                        stat.author = rs.getString("author");
                        stat.views = rs.getInt("views");
                        stat.likes = rs.getInt("likes");
                        stat.createdAt = formatDate(rs.getTimestamp("created_at"));
                        stats.add(stat);
                    }
                }
            }

            return stats;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public java.util.List<ama.servlets.StatisticsServlet.ActivityStatistic> getRecentActivity(int days) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT DATE(a.created_at) as date,
                       COUNT(*) as new_abbreviations,
                       COALESCE(SUM(a.views), 0) as total_views,
                       COALESCE(SUM(a.likes), 0) as total_likes
                FROM abbreviations a
                WHERE a.created_at >= CURRENT_DATE - INTERVAL '7 days'
                GROUP BY DATE(a.created_at)
                ORDER BY DATE(a.created_at) DESC
                LIMIT 7
                """;

            java.util.List<ama.servlets.StatisticsServlet.ActivityStatistic> stats = new java.util.ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ama.servlets.StatisticsServlet.ActivityStatistic stat = new ama.servlets.StatisticsServlet.ActivityStatistic();
                    stat.date = rs.getString("date");
                    stat.newAbbreviations = rs.getInt("new_abbreviations");
                    stat.totalViews = rs.getInt("total_views");
                    stat.totalLikes = rs.getInt("total_likes");
                    stats.add(stat);
                }
            }

            return stats;
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "N/A";

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy");
        return sdf.format(new java.util.Date(timestamp.getTime()));
    }

    private List<String> getMeaningsForAbbreviation(Connection conn, int abbreviationId) throws SQLException {
        String sql = "SELECT meaning FROM abbreviation_meanings WHERE abbreviation_id = ? ORDER BY sort_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, abbreviationId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> meanings = new ArrayList<>();
                while (rs.next()) {
                    meanings.add(rs.getString("meaning"));
                }
                return meanings;
            }
        }
    }

    ///  mapam resultSet la Abbreviation
    private Abbreviation mapResultSetToAbbreviation(ResultSet rs) throws SQLException {
        Abbreviation abbr = new Abbreviation();
        abbr.setId(rs.getInt("id"));
        abbr.setName(rs.getString("name"));
        abbr.setDescription(rs.getString("description"));
        abbr.setViews(rs.getInt("views"));
        abbr.setLikes(rs.getInt("likes"));
        abbr.setFavorites(rs.getInt("favorites"));
        abbr.setCreatedAt(rs.getTimestamp("created_at"));

        // setam relatiile
        try {
            abbr.setLanguageId(rs.getInt("language_id"));
            abbr.setDomainId(rs.getInt("domain_id"));
            abbr.setUserId(rs.getInt("user_id"));
            abbr.setDocbook(rs.getString("docbook"));

            Language lang = new Language();
            lang.setCode(rs.getString("lang_code"));
            lang.setName(rs.getString("lang_name"));
            abbr.setLanguage(lang);

            Domain domain = new Domain();
            domain.setCode(rs.getString("domain_code"));
            domain.setName(rs.getString("domain_name"));
            abbr.setDomain(domain);

            User user = new User();
            user.setUsername(rs.getString("username"));
            abbr.setUser(user);
        } catch (SQLException e) {
        }

        return abbr;
    }
}