package ama.database.dao;

import ama.database.DatabaseManager;
import ama.models.User;
import java.sql.*;
import java.util.Optional;

public class UserDAO {
    private DatabaseManager dbManager;

    public UserDAO() {
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException | java.io.IOException e) {
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
    }

    public Optional<User> findById(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "SELECT id, username, email, password, role, created_at FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToUser(rs));
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

    public User create(User user) throws SQLException {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty() ||
                user.getEmail() == null || user.getEmail().trim().isEmpty() ||
                user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("User data is incomplete");
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?) RETURNING id, created_at";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getPassword());
                stmt.setString(4, user.getRole() != null ? user.getRole() : "user");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        user.setId(rs.getInt("id"));
                        user.setCreatedAt(rs.getTimestamp("created_at"));
                        return user;
                    }
                    throw new SQLException("Failed to create user");
                }
            }
        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
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

    public boolean emailExists(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
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

    public Optional<User> authenticate(String usernameOrEmail) throws SQLException {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            return Optional.empty();
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
                SELECT id, username, email, password, role, created_at 
                FROM users 
                WHERE username = ? OR email = ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, usernameOrEmail);
                stmt.setString(2, usernameOrEmail);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToUser(rs));
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

    /// daca au adaugat macar o abreviere
    public int getTotalActiveUsers() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
            SELECT COUNT(DISTINCT u.id) 
            FROM users u 
            JOIN abbreviations a ON u.id = a.user_id
            """;

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

    public java.util.List<ama.servlets.StatisticsServlet.UserStatistic> getTopContributors(int limit) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            String sql = """
            SELECT u.username,
                   COUNT(a.id) as contributions_count,
                   COALESCE(SUM(a.views), 0) as total_views,
                   COALESCE(SUM(a.likes), 0) as total_likes
            FROM users u
            JOIN abbreviations a ON u.id = a.user_id
            GROUP BY u.id, u.username
            ORDER BY contributions_count DESC, total_views DESC
            LIMIT ?
            """;

            java.util.List<ama.servlets.StatisticsServlet.UserStatistic> stats = new java.util.ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ama.servlets.StatisticsServlet.UserStatistic stat = new ama.servlets.StatisticsServlet.UserStatistic();
                        stat.username = rs.getString("username");
                        stat.contributionsCount = rs.getInt("contributions_count");
                        stat.totalViews = rs.getInt("total_views");
                        stat.totalLikes = rs.getInt("total_likes");
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

    /// mapam rezultatul din ResultSet la User
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}