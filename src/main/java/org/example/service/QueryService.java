package org.example.service;

import lombok.Getter;
import lombok.Setter;
import org.example.entity.Query;
import org.example.util.JsonFileHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryService {
    private final DatabaseService databaseService;
    private final List<Query> savedQueries;
    private static final String QUERIES_FILE = "saved_queries.json";

    public QueryService(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.savedQueries = loadSavedQueries();

        if (savedQueries.isEmpty()) {
            initializeSampleQueries();
            saveQueriesToFile();
        }
    }

    private void initializeSampleQueries() {
        savedQueries.add(new Query("All clients",
                "SELECT * FROM clients;"));
        savedQueries.add(new Query("All employees",
                "SELECT * FROM employees;"));
    }

    private List<Query> loadSavedQueries() {
        try {
            List<Query> queries = JsonFileHandler.readFromFile(QUERIES_FILE, Query.class);
            return queries != null ? queries : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error loading saved queries: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveQueriesToFile() {
        try {
            JsonFileHandler.writeToFile(QUERIES_FILE, savedQueries);
        } catch (Exception e) {
            System.err.println("Error saving queries to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public QueryResult executeQuery(String sqlQuery) {
        QueryResult result = new QueryResult();

        try (Connection conn = databaseService.getConnection()) {

            try (PreparedStatement statement = conn.prepareStatement(sqlQuery)) {

                boolean hasResults = statement.execute();

                if (hasResults) {
                    try (ResultSet resultSet = statement.getResultSet()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            result.getColumns().add(metaData.getColumnName(i));
                        }

                        while (resultSet.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnName(i);
                                row.put(columnName, resultSet.getObject(i));
                            }
                            result.getData().add(row);
                        }

                        result.setSuccess(true);
                        result.setMessage("Запрос выполнен успешно. Найдено строк: " + result.getData().size());
                    }
                } else {
                    int affectedRows = statement.getUpdateCount();
                    result.setSuccess(true);
                    result.setMessage("Запрос выполнен. Затронуто строк: " + affectedRows);
                }
            }

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setMessage("Ошибка выполнения запроса: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void saveQuery(String name, String query, String description) {
        deleteSavedQuery(name);

        Query savedQuery = new Query(name, query);
        savedQuery.setDescription(description);
        savedQueries.add(savedQuery);
        saveQueriesToFile();
    }

    public List<Query> getSavedQueries() {
        return new ArrayList<>(savedQueries);
    }

    public void deleteSavedQuery(String name) {
        boolean removed = savedQueries.removeIf(q -> q.getName().equals(name));
        if (removed) {
            saveQueriesToFile();
        }
    }

    @Getter
    public static class QueryResult {
        @Setter
        private boolean success;
        @Setter
        private String message;
        private final List<String> columns;
        private final List<Map<String, Object>> data;

        public QueryResult() {
            this.columns = new ArrayList<>();
            this.data = new ArrayList<>();
        }
    }
}
