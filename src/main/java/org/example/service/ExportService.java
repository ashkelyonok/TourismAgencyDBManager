package org.example.service;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExportService {
    private final DatabaseService databaseService;
    private static final String EXPORT_DIR = "exports";
    private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";

    public ExportService(DatabaseService databaseService) {
        this.databaseService = databaseService;
        createExportDirectory();
    }

    private void createExportDirectory() {
        try {
            Path exportPath = Paths.get(EXPORT_DIR);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
                System.out.println("Created export directory: " + exportPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error creating export directory: " + e.getMessage());
        }
    }

    public ExportResult exportCurrentSchema() {
        ExportResult result = new ExportResult();

        try {
            String schema = databaseService.getCurrentSchema();
            String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            String fileName = String.format("schema_%s_%s.xlsx", schema, timestamp);
            Path filePath = Paths.get(EXPORT_DIR, fileName);

            List<String> tables = databaseService.getTablesInCurrentSchema();

            if (tables.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No tables found in schema: " + schema);
                return result;
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                createSchemaInfoSheet(workbook, schema, tables);

                for (String table : tables) {
                    exportTableToSheet(workbook, table);
                }

                try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                    workbook.write(outputStream);
                }

                result.setSuccess(true);
                result.setMessage("Schema exported successfully: " + fileName);
                result.setExportFile(filePath.toFile());
                result.setTablesCount(tables.size());

            } catch (IOException e) {
                result.setSuccess(false);
                result.setMessage("Error writing export file: " + e.getMessage());
            }

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setMessage("Database error during export: " + e.getMessage());
        }

        return result;
    }

    public ExportResult exportTable(String tableName) {
        ExportResult result = new ExportResult();

        try {
            String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            String fileName = String.format("table_%s_%s.xlsx", tableName, timestamp);
            Path filePath = Paths.get(EXPORT_DIR, fileName);

            try (Workbook workbook = new XSSFWorkbook()) {
                exportTableToSheet(workbook, tableName);

                try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                    workbook.write(outputStream);
                }

                result.setSuccess(true);
                result.setMessage("Table exported successfully: " + fileName);
                result.setExportFile(filePath.toFile());
                result.setTablesCount(1);

            } catch (IOException e) {
                result.setSuccess(false);
                result.setMessage("Error writing export file: " + e.getMessage());
            }

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setMessage("Database error during export: " + e.getMessage());
        }

        return result;
    }

    public ExportResult exportQueryResults(String queryName, List<String> columns, List<Map<String, Object>> data) {
        ExportResult result = new ExportResult();

        try {
            String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            String fileName = String.format("query_%s_%s.xlsx",
                    queryName.replaceAll("[^a-zA-Z0-9]", "_"), timestamp);
            Path filePath = Paths.get(EXPORT_DIR, fileName);

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Query Results");

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                    cell.setCellStyle(createHeaderStyle(workbook));
                }

                for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                    Row row = sheet.createRow(rowIndex + 1);
                    Map<String, Object> rowData = data.get(rowIndex);

                    for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                        String columnName = columns.get(colIndex);
                        Object value = rowData.get(columnName);
                        Cell cell = row.createCell(colIndex);

                        if (value != null) {
                            setCellValue(cell, value);
                        }
                    }
                }

                autoSizeColumns(sheet, columns.size());

                try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                    workbook.write(outputStream);
                }

                result.setSuccess(true);
                result.setMessage("Query results exported successfully: " + fileName);
                result.setExportFile(filePath.toFile());
                result.setTablesCount(1);

            } catch (IOException e) {
                result.setSuccess(false);
                result.setMessage("Error writing export file: " + e.getMessage());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error during export: " + e.getMessage());
        }

        return result;
    }

    private void createSchemaInfoSheet(Workbook workbook, String schema, List<String> tables) {
        Sheet sheet = workbook.createSheet("Schema Info");

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Database Schema Export");

        Row schemaRow = sheet.createRow(2);
        schemaRow.createCell(0).setCellValue("Schema:");
        schemaRow.createCell(1).setCellValue(schema);

        Row dateRow = sheet.createRow(3);
        dateRow.createCell(0).setCellValue("Export Date:");
        dateRow.createCell(1).setCellValue(new Date().toString());

        Row tablesCountRow = sheet.createRow(4);
        tablesCountRow.createCell(0).setCellValue("Tables Count:");
        tablesCountRow.createCell(1).setCellValue(tables.size());

        Row tablesHeaderRow = sheet.createRow(6);
        tablesHeaderRow.createCell(0).setCellValue("Tables in schema:");

        for (int i = 0; i < tables.size(); i++) {
            Row row = sheet.createRow(7 + i);
            row.createCell(0).setCellValue((i + 1) + ".");
            row.createCell(1).setCellValue(tables.get(i));
        }

        autoSizeColumns(sheet, 2);
    }

    private void exportTableToSheet(Workbook workbook, String tableName) throws SQLException {
        Sheet sheet = workbook.createSheet(tableName);

        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            Row headerRow = sheet.createRow(0);
            for (int i = 1; i <= columnCount; i++) {
                Cell cell = headerRow.createCell(i - 1);
                cell.setCellValue(metaData.getColumnName(i));
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            int rowIndex = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = row.createCell(i - 1);
                    Object value = rs.getObject(i);
                    setCellValue(cell, value);
                }
            }

            autoSizeColumns(sheet, columnCount);

        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @Setter
    @Getter
    public static class ExportResult {
        private boolean success;
        private String message;
        private File exportFile;
        private int tablesCount;

    }
}