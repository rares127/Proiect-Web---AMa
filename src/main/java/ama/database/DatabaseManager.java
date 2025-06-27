package ama.database;

import java.sql.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
/// gestioneaza conexiunile la BD si crearea connection pool

public class DatabaseManager {
    private static DatabaseManager instance;
    private final Properties config;
    private final ConcurrentLinkedQueue<Connection> connectionPool;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maxPoolSize;

    private DatabaseManager() throws SQLException, IOException {
        this.config = new Properties();
        loadConfiguration();

        this.jdbcUrl = config.getProperty("db.url");
        this.username = config.getProperty("db.username");
        this.password = config.getProperty("db.password");
        this.maxPoolSize = Integer.parseInt(config.getProperty("db.pool.maxActive", "20"));

        this.connectionPool = new ConcurrentLinkedQueue<>();
        initializeConnectionPool();
    }

    public static synchronized DatabaseManager getInstance() throws SQLException, IOException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadConfiguration() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (is == null) {
                throw new IOException("Nu s-a găsit fișierul database.properties în classpath");
            }
            config.load(is);
        }
    }

    private void initializeConnectionPool() throws SQLException {
        int initialSize = Integer.parseInt(config.getProperty("db.pool.initialSize", "5"));
        for (int i = 0; i < initialSize; i++) {
            connectionPool.offer(createNewConnection());
        }
    }

    private Connection createNewConnection() throws SQLException {
        try {
            Class.forName(config.getProperty("db.driver"));
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = connectionPool.poll();
        if (connection == null || connection.isClosed()) {
            connection = createNewConnection();
        }
        return connection;
    }

    public void releaseConnection(Connection connection) {
        if (connection != null && connectionPool.size() < maxPoolSize) {
            try {
                if (!connection.isClosed()) {
                    connectionPool.offer(connection);
                }
            } catch (SQLException e) {
                try {
                    connection.close();
                } catch (SQLException closeEx) {
                }
            }
        }
    }
}