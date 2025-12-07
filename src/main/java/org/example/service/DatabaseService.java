package org.example.service;

import lombok.Getter;
import org.example.util.EnvConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {
    private String url;
    private String user;
    private String password;
    @Getter
    private String currentSchema;

    public DatabaseService() {
        try {
            this.url = EnvConfig.get("JDBC_URL", null);
            this.user = EnvConfig.get("DB_USER", null);
            this.password = EnvConfig.get("DB_PASSWORD", null);

            if (url == null) throw new RuntimeException("JDBC_URL not configured");

            try (Connection testConn = createNewConnection()) {
            }

            String envSchema = EnvConfig.get("DB_SCHEMA", "public");
            this.currentSchema = envSchema;

        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to DB: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = createNewConnection();
        if (currentSchema != null) {
            try (Statement st = conn.createStatement()) {
                st.execute("SET search_path TO " + currentSchema);
            }
        }
        return conn;
    }

    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String getDatabaseName() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String url = md.getURL();
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0) return url.substring(lastSlash + 1);
            return url;
        } catch (SQLException e) {
            return "unknown";
        }
    }

    public List<String> getSchemas() throws SQLException {
        List<String> schemas = new ArrayList<>();
        String sql = "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) schemas.add(rs.getString(1));
        }
        return schemas;
    }

    public void setSchema(String schema) {
        if (!schema.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
        this.currentSchema = schema;
    }

    public List<String> getTablesInCurrentSchema() throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type='BASE TABLE' ORDER BY table_name";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, currentSchema == null ? "public" : currentSchema);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    public QueryResult fetchPreview(String tableName, int limit) throws SQLException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Table name is required");
        }
        if (!isSafeIdentifier(tableName)) {
            throw new IllegalArgumentException("Invalid table name");
        }
        if (limit <= 0) limit = 100;

        String sql = "SELECT * FROM " + tableName + " LIMIT " + limit;

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            return new QueryResult(columns, rows);
        }
    }

    private boolean isSafeIdentifier(String ident) {
        return ident != null && ident.matches("[A-Za-z0-9_]+");
    }

    public void close() {
    }

    @Getter
    public static class QueryResult {
        private final List<String> columns;
        private final List<Map<String, Object>> rows;

        public QueryResult(List<String> columns, List<Map<String, Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }

    }
}
