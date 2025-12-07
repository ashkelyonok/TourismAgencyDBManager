package org.example.service;

import java.sql.SQLException;
import java.util.List;

public class SchemaService {
    private final DatabaseService databaseService;

    public SchemaService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public List<String> getAvailableSchemas() throws SQLException {
        return databaseService.getSchemas();
    }

    public void switchToSchema(String schema) throws SQLException {
        databaseService.setSchema(schema);
    }

    public String getCurrentSchema(){
        return databaseService.getCurrentSchema();
    }
}
