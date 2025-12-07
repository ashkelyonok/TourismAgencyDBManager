package org.example.service;

import org.example.entity.Column;
import org.example.entity.Table;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableService {
    private final DatabaseService databaseService;

    public TableService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    // ========== МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ДАННЫХ ==========

    public Table getTableInfo(String tableName) throws SQLException {
        System.out.println("Loading table info for: " + tableName);
        System.out.println("Current schema: " + databaseService.getCurrentSchema());

        Table table = new Table(tableName);

        try (Connection conn = databaseService.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet tables = metaData.getTables(null, databaseService.getCurrentSchema(), tableName, new String[]{"TABLE"})) {
                if (!tables.next()) {
                    throw new SQLException("Table '" + tableName + "' not found in schema '" + databaseService.getCurrentSchema() + "'");
                }
            }

            List<String> primaryKeys = getPrimaryKeys(metaData, tableName);
            System.out.println("Primary keys: " + primaryKeys);

            Map<String, String[]> foreignKeys = getForeignKeys(metaData, tableName);
            System.out.println("Foreign keys: " + foreignKeys);

            getColumnInfo(metaData, tableName, table, primaryKeys, foreignKeys);

            System.out.println("Loaded " + table.getColumns().size() + " columns");
        } catch (SQLException e) {
            System.err.println("SQL Error in getTableInfo: " + e.getMessage());
            throw e;
        }

        return table;
    }

    // ========== CRUD ОПЕРАЦИИ С ДАННЫМИ (ОСНОВНЫЕ МЕТОДЫ) ==========

    public boolean insertData(String tableName, Map<String, Object> values) throws SQLException {
        validateRequiredFields(tableName, values);
        return executeInsert(tableName, values);
    }

    public boolean updateData(String tableName, Map<String, Object> oldData, Map<String, Object> newData) throws SQLException {
        String primaryKey = findPrimaryKeyColumn(tableName);

        if (primaryKey != null && oldData.containsKey(primaryKey)) {
            return executeUpdateByPrimaryKey(tableName, newData, primaryKey, oldData.get(primaryKey));
        } else {
            return executeUpdateByAllFields(tableName, oldData, newData);
        }
    }

    public boolean deleteData(String tableName, Map<String, Object> recordData) throws SQLException {
        String primaryKey = findPrimaryKeyColumn(tableName);

        if (primaryKey != null && recordData.containsKey(primaryKey)) {
            return executeDeleteByPrimaryKey(tableName, primaryKey, recordData.get(primaryKey));
        } else {
            return executeDeleteByAllFields(tableName, recordData);
        }
    }

    // ========== БАЗОВЫЕ SQL ОПЕРАЦИИ (приватные) ==========

    private boolean executeInsert(String tableName, Map<String, Object> values) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = buildInsertSQL(tableName, values);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                setParameters(statement, new ArrayList<>(values.values()));
                return statement.executeUpdate() > 0;
            }
        }
    }

    private boolean executeUpdateByPrimaryKey(String tableName, Map<String, Object> newData,
                                              String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = buildUpdateSQLByPrimaryKey(tableName, newData, primaryKeyColumn);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                List<Object> parameters = new ArrayList<>(newData.values());
                parameters.add(primaryKeyValue);
                setParameters(statement, parameters);
                return statement.executeUpdate() > 0;
            }
        }
    }

    private boolean executeUpdateByAllFields(String tableName, Map<String, Object> oldData,
                                             Map<String, Object> newData) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = buildUpdateSQLByAllFields(tableName, oldData, newData);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                List<Object> parameters = new ArrayList<>();
                parameters.addAll(newData.values());
                parameters.addAll(oldData.values());
                setParameters(statement, parameters);
                return statement.executeUpdate() > 0;
            }
        }
    }

    private boolean executeDeleteByPrimaryKey(String tableName, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = buildDeleteSQLByPrimaryKey(tableName, primaryKeyColumn);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setObject(1, primaryKeyValue);
                return statement.executeUpdate() > 0;
            }
        }
    }

    private boolean executeDeleteByAllFields(String tableName, Map<String, Object> recordData) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = buildDeleteSQLByAllFields(tableName, recordData);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                setParameters(statement, new ArrayList<>(recordData.values()));
                return statement.executeUpdate() > 0;
            }
        }
    }

    // ========== ПОСТРОЕНИЕ SQL ЗАПРОСОВ ==========

    private String buildInsertSQL(String tableName, Map<String, Object> values) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        sql.append(String.join(", ", values.keySet()));
        sql.append(") VALUES (");
        sql.append("?".repeat(values.size()).replaceAll(".(?=.)", "$0, "));
        sql.append(")");
        return sql.toString();
    }

    private String buildUpdateSQLByPrimaryKey(String tableName, Map<String, Object> values, String primaryKeyColumn) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");

        List<String> setClauses = new ArrayList<>();
        for (String column : values.keySet()) {
            if (!column.equals(primaryKeyColumn)) {
                setClauses.add(column + " = ?");
            }
        }

        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
        return sql.toString();
    }

    private String buildUpdateSQLByAllFields(String tableName, Map<String, Object> oldData, Map<String, Object> newData) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");

        List<String> setClauses = new ArrayList<>();
        for (String column : newData.keySet()) {
            setClauses.add(column + " = ?");
        }

        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE ");

        List<String> whereClauses = new ArrayList<>();
        for (String column : oldData.keySet()) {
            whereClauses.add(column + " = ?");
        }
        sql.append(String.join(" AND ", whereClauses));

        return sql.toString();
    }

    private String buildDeleteSQLByPrimaryKey(String tableName, String primaryKeyColumn) {
        return "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
    }

    private String buildDeleteSQLByAllFields(String tableName, Map<String, Object> recordData) {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(tableName).append(" WHERE ");

        List<String> whereClauses = new ArrayList<>();
        for (String column : recordData.keySet()) {
            whereClauses.add(column + " = ?");
        }
        sql.append(String.join(" AND ", whereClauses));

        return sql.toString();
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void validateRequiredFields(String tableName, Map<String, Object> values) throws SQLException {
        Table tableInfo = getTableInfo(tableName);
        for (Column column : tableInfo.getColumns()) {
            if (!column.isNullable() && column.getDefaultValue() == null &&
                    !values.containsKey(column.getName())) {
                throw new SQLException("Обязательное поле '" + column.getName() + "' не заполнено");
            }
        }
    }

    public String findPrimaryKeyColumn(String tableName) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet pkResult = metaData.getPrimaryKeys(null, databaseService.getCurrentSchema(), tableName)) {
                if (pkResult.next()) {
                    return pkResult.getString("COLUMN_NAME");
                }
            }
        }
        return null;
    }

    private void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    // ========== МЕТАДАННЫЕ ТАБЛИЦ ==========

    private List<String> getPrimaryKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet pkResult = metaData.getPrimaryKeys(null, databaseService.getCurrentSchema(), tableName)) {
            while (pkResult.next()) {
                primaryKeys.add(pkResult.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }

    private Map<String, String[]> getForeignKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        Map<String, String[]> foreignKeys = new HashMap<>();
        try (ResultSet fkResult = metaData.getImportedKeys(null, databaseService.getCurrentSchema(), tableName)) {
            while (fkResult.next()) {
                String fkColumn = fkResult.getString("FKCOLUMN_NAME");
                String pkTable = fkResult.getString("PKTABLE_NAME");
                String pkColumn = fkResult.getString("PKCOLUMN_NAME");
                foreignKeys.put(fkColumn, new String[]{pkTable, pkColumn});
            }
        }
        return foreignKeys;
    }

    private void getColumnInfo(DatabaseMetaData metaData, String tableName, Table table,
                               List<String> primaryKeys, Map<String, String[]> foreignKeys) throws SQLException {
        try (ResultSet columnsResult = metaData.getColumns(null, databaseService.getCurrentSchema(), tableName, null)) {
            if (!columnsResult.isBeforeFirst()) {
                throw new SQLException("No columns found for table '" + tableName + "' in schema '" + databaseService.getCurrentSchema() + "'");
            }

            while (columnsResult.next()) {
                String columnName = columnsResult.getString("COLUMN_NAME");
                String columnType = columnsResult.getString("TYPE_NAME");
                String defaultValue = columnsResult.getString("COLUMN_DEF");
                String isNullable = columnsResult.getString("IS_NULLABLE");

                System.out.println("Column: " + columnName + ", Type: " + columnType + ", Nullable: " + isNullable);

                Column column = new Column(columnName, columnType);
                column.setPrimaryKey(primaryKeys.contains(columnName));
                column.setNullable("YES".equals(isNullable));
                column.setDefaultValue(defaultValue);

                if (foreignKeys.containsKey(columnName)) {
                    String[] fkInfo = foreignKeys.get(columnName);
                    column.setForeignKeyTable(fkInfo[0]);
                    column.setForeignKeyColumn(fkInfo[1]);
                }

                table.addColumn(column);
            }
        }
    }

    // ========== ОПЕРАЦИИ С СТРУКТУРОЙ ТАБЛИЦ ==========

    public boolean createTable(String tableName, List<Column> columns) throws SQLException {
        try (Connection conn = databaseService.getConnection();
             Statement statement = conn.createStatement()) {

            String sql = buildCreateTableSQL(tableName, columns);
            statement.execute(sql);
            return true;
        }
    }

    private String buildCreateTableSQL(String tableName, List<Column> columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableName).append(" (");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeyColumns = new ArrayList<>();
        List<String> foreignKeyConstraints = new ArrayList<>();

        for (Column column : columns) {
            StringBuilder columnDef = new StringBuilder();
            columnDef.append(column.getName()).append(" ").append(column.getType());

            if (!column.getType().toUpperCase().contains("SERIAL")) {
                if (!column.isNullable()) {
                    columnDef.append(" NOT NULL");
                }
            }

            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                String defaultValue = column.getDefaultValue().trim();
                if (!isSqlFunction(defaultValue) && !isSqlKeyword(defaultValue) &&
                        !defaultValue.matches("-?\\d+(\\.\\d+)?")) {
                    columnDef.append(" DEFAULT '").append(defaultValue).append("'");
                } else {
                    columnDef.append(" DEFAULT ").append(defaultValue);
                }
            }

            columnDefinitions.add(columnDef.toString());

            if (column.isPrimaryKey()) {
                primaryKeyColumns.add(column.getName());
            }

            if (column.getForeignKeyTable() != null && column.getForeignKeyColumn() != null) {
                String fkName = "fk_" + tableName + "_" + column.getName();
                foreignKeyConstraints.add("CONSTRAINT " + fkName + " FOREIGN KEY (" + column.getName() +
                        ") REFERENCES " + column.getForeignKeyTable() + "(" + column.getForeignKeyColumn() + ")");
            }
        }

        if (!primaryKeyColumns.isEmpty()) {
            columnDefinitions.add("PRIMARY KEY (" + String.join(", ", primaryKeyColumns) + ")");
        }
        columnDefinitions.addAll(foreignKeyConstraints);

        sql.append(String.join(", ", columnDefinitions));
        sql.append(")");

        return sql.toString();
    }

    public boolean dropTable(String tableName) throws SQLException {
        try (Connection conn = databaseService.getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute("DROP TABLE " + tableName);
            return true;
        }
    }

    private boolean isSqlFunction(String value) {
        if (value == null) return false;
        String upperValue = value.toUpperCase();
        return upperValue.contains("(") && upperValue.contains(")") ||
                upperValue.startsWith("NOW()") ||
                upperValue.startsWith("CURRENT_DATE") ||
                upperValue.startsWith("CURRENT_TIMESTAMP") ||
                upperValue.startsWith("GEN_RANDOM_UUID()");
    }

    private boolean isSqlKeyword(String value) {
        if (value == null) return false;
        String upperValue = value.toUpperCase();
        return upperValue.equals("TRUE") || upperValue.equals("FALSE") ||
                upperValue.equals("NULL") || upperValue.equals("CURRENT_DATE") ||
                upperValue.equals("CURRENT_TIMESTAMP") || upperValue.equals("NOW()") ||
                upperValue.startsWith("NEXTVAL("); // Для последовательностей SERIAL
    }
}
