package org.example.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.example.component.*;
import org.example.entity.Column;
import org.example.service.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {

    @FXML private VBox navPanel;
    @FXML private ToolBar mainToolbar;
    @FXML private ToolBar extendedToolbar;
    @FXML private ComboBox<String> schemaCombo;
    @FXML private Label currentSchemaLabel;
    @FXML private Label dbNameLabel;
    @FXML private ListView<String> tablesList;
    @FXML private TableView<Map<String, Object>> tableView;
    @FXML private Label statusLabel;

    // Query tab
    @FXML private TextArea queryTextArea;
    @FXML private TableView<Object> queryResultTable;
    @FXML private ComboBox<String> savedQueriesCombo;
    @FXML private Label currentTableLabel;

    @FXML private Button editRecordButton;
    @FXML private Button addRecordButton;
    @FXML private Button deleteRecordButton;

    private DatabaseService databaseService;
    private SchemaService schemaService;
    private TableService tableService;
    private NavigationService navigationService;
    private QueryService queryService;
    private BackupService backupService;
    private ExportService exportService;

    private String currentTable;
    private boolean isInitialized = false;

    public void setServices(DatabaseService databaseService, SchemaService schemaService,
                            TableService tableService, NavigationService navigationService,
                            QueryService queryService, BackupService backupService,
                            ExportService exportService) {
        this.databaseService = databaseService;
        this.schemaService = schemaService;
        this.tableService = tableService;
        this.navigationService = navigationService;
        this.queryService = queryService;
        this.backupService = backupService;
        this.exportService = exportService;


        if (isInitialized) {
            setupNavigation();
            setupQueryTab();
        }
        dbNameLabel.setText(databaseService.getDatabaseName());
        Platform.runLater(this::initializeData);
    }

    @FXML
    public void initialize() {
        isInitialized = true;

        tablesList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onTableSelected(newVal)
        );

        tableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateButtonsState()
        );

        setupKeyboardShortcuts();
        if (navigationService != null) {
            setupNavigation();
        }
    }

    private void setupKeyboardShortcuts() {
        queryTextArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F5:
                    executeQuery();
                    event.consume();
                    break;
                case F9:
                    saveCurrentQuery();
                    event.consume();
                    break;
                case DELETE:
                    if (event.isControlDown()) {
                        deleteSavedQuery();
                        event.consume();
                    }
                    break;
            }
        });

        statusLabel.setText("Горячие клавиши: F5 - выполнить запрос, F9 - сохранить запрос, Ctrl+Del - удалить запрос");
    }

    private void updateButtonsState() {
        boolean hasSelection = tableView.getSelectionModel().getSelectedItem() != null;
        boolean tableSelected = currentTable != null;

        editRecordButton.setDisable(!hasSelection || !tableSelected);
        deleteRecordButton.setDisable(!hasSelection || !tableSelected);
        addRecordButton.setDisable(!tableSelected);
    }

    private void initializeData() {
        try {
            loadSchemas();
            statusLabel.setText("Подключено к базе данных");

            updateButtonsState();
        } catch (Exception e) {
            showAlert("Ошибка инициализации", e.getMessage());
        }
    }

    private void loadSchemas() {
        try {
            List<String> schemas = schemaService.getAvailableSchemas();

            String currentSelection = schemaCombo.getValue();

            schemaCombo.getItems().setAll(schemas);

            String current = schemaService.getCurrentSchema();
            if (current != null && schemas.contains(current)) {
                schemaCombo.setValue(current);
                currentSchemaLabel.setText("Текущая: " + current);
                loadTablesForCurrentSchema();
            } else if (currentSelection != null && schemas.contains(currentSelection)) {
                schemaCombo.setValue(currentSelection);
            } else if (!schemas.isEmpty()) {
                schemaCombo.setValue(schemas.get(0));
            }

        } catch (Exception ex) {
            showAlert("Ошибка загрузки схем", ex.getMessage());
        }
    }

    @FXML
    private void onSchemaSelected() {
        String schema = schemaCombo.getValue();
        if (schema == null) return;

        try {
            schemaService.switchToSchema(schema);
            currentSchemaLabel.setText("Текущая: " + schema);
            refreshAfterTableOperation();
            statusLabel.setText("Схема изменена на: " + schema);
        } catch (Exception ex) {
            showAlert("Ошибка установки схемы", ex.getMessage());
            loadSchemas();
        }
    }

    private void loadTablesForCurrentSchema() {
        try {
            List<String> tables = databaseService.getTablesInCurrentSchema();
            tablesList.getItems().setAll(tables);

            tablesList.getSelectionModel().clearSelection();
            currentTable = null;
            if (currentTableLabel != null) {
                currentTableLabel.setText("Выберите таблицу для просмотра данных");
            }
            tableView.getItems().clear();
            tableView.getColumns().clear();

        } catch (Exception ex) {
            showAlert("Ошибка загрузки таблиц", ex.getMessage());
        }
    }

    private void onTableSelected(String tableName) {
        if (tableName != null && !tableName.equals(currentTable)) {
            currentTable = tableName;
            if (currentTableLabel != null) {
                currentTableLabel.setText("Таблица: " + tableName);
            }
            loadTablePreview(tableName);
            updateButtonsState();
        }
    }

    private void loadTablePreview(String tableName) {
        try {
            DatabaseService.QueryResult result = databaseService.fetchPreview(tableName, 100);

            tableView.getItems().clear();
            tableView.getColumns().clear();

            List<String> columns = result.getColumns();

            List<Map<String, Object>> sortedRows = new ArrayList<>(result.getRows());
            if (!sortedRows.isEmpty() && sortedRows.get(0).containsKey("id")) {
                sortedRows.sort((row1, row2) -> {
                    Object id1 = row1.get("id");
                    Object id2 = row2.get("id");
                    if (id1 instanceof Number && id2 instanceof Number) {
                        return Long.compare(((Number) id1).longValue(), ((Number) id2).longValue());
                    }
                    return 0;
                });
            }

            for (String colName : columns) {
                TableColumn<Map<String, Object>, Object> col = new TableColumn<>(colName);
                col.setCellValueFactory(cellData -> {
                    Map<String, Object> row = cellData.getValue();
                    Object val = row.get(colName);
                    return new SimpleObjectProperty<>(val);
                });
                tableView.getColumns().add(col);
            }

            for (Map<String, Object> row : sortedRows) {
                Map<String, Object> safeRow = new HashMap<>();
                for (String col : columns) {
                    safeRow.put(col, row.getOrDefault(col, null));
                }
                tableView.getItems().add(safeRow);
            }

            statusLabel.setText("Загружена таблица: " + tableName);

        } catch (IllegalArgumentException iae) {
            showAlert("Неверное имя таблицы", iae.getMessage());
        } catch (Exception ex) {
            showAlert("Ошибка превью", ex.getMessage());
        }
    }

    @FXML
    private void editSelectedRecord() {
        Map<String, Object> selectedRecord = tableView.getSelectionModel().getSelectedItem();

        if (selectedRecord == null) {
            showAlert("Ошибка", "Выберите запись для редактирования");
            return;
        }

        if (currentTable == null) {
            showAlert("Ошибка", "Таблица не выбрана");
            return;
        }

        try {
            org.example.entity.Table tableInfo = tableService.getTableInfo(currentTable);

            EditRecordDialog dialog = new EditRecordDialog(currentTable, selectedRecord, tableInfo.getColumns());

            dialog.showAndWait().ifPresent(updatedData -> {
                if (updatedData != null && !updatedData.equals(selectedRecord)) {
                    updateRecordInDatabase(currentTable, selectedRecord, updatedData);
                }
            });

        } catch (Exception e) {
            System.err.println("Failed to load table info, trying alternative approach: " + e.getMessage());
            e.printStackTrace();

            try {
                List<Column> dynamicColumns = createDynamicColumnsFromPreview();
                EditRecordDialog dialog = new EditRecordDialog(currentTable, selectedRecord, dynamicColumns);

                dialog.showAndWait().ifPresent(updatedData -> {
                    if (updatedData != null && !updatedData.equals(selectedRecord)) {
                        updateRecordInDatabase(currentTable, selectedRecord, updatedData);
                    }
                });

            } catch (Exception ex) {
                System.err.println("Alternative approach also failed: " + ex.getMessage());
                ex.printStackTrace();

                try {
                    List<Column> simpleColumns = createSimpleColumnsFromRecord(selectedRecord);
                    EditRecordDialog dialog = new EditRecordDialog(currentTable, selectedRecord, simpleColumns);

                    dialog.showAndWait().ifPresent(updatedData -> {
                        if (updatedData != null && !updatedData.equals(selectedRecord)) {
                            updateRecordInDatabase(currentTable, selectedRecord, updatedData);
                        }
                    });

                } catch (Exception finalEx) {
                    String errorMessage = "Не удалось загрузить информацию о таблице: " + e.getMessage();
                    showAlert("Ошибка", errorMessage);
                }
            }
        }
    }

    private List<Column> createDynamicColumnsFromPreview() {
        List<Column> columns = new ArrayList<>();

        for (TableColumn<Map<String, Object>, ?> tableColumn : tableView.getColumns()) {
            String columnName = tableColumn.getText();
            Column column = new Column(columnName, "VARCHAR");

            try {
                String primaryKey = tableService.findPrimaryKeyColumn(currentTable);
                column.setPrimaryKey(columnName.equals(primaryKey));
            } catch (Exception e) {
                column.setPrimaryKey(false);
            }

            column.setNullable(true);
            columns.add(column);
        }

        return columns;
    }

    private List<Column> createSimpleColumnsFromRecord(Map<String, Object> recordData) {
        List<Column> columns = new ArrayList<>();

        for (String columnName : recordData.keySet()) {
            Column column = new Column(columnName, "TEXT");

            boolean isPrimaryKey = columnName.equalsIgnoreCase("id") ||
                    columnName.toLowerCase().endsWith("_id") ||
                    columnName.equalsIgnoreCase("pk");
            column.setPrimaryKey(isPrimaryKey);

            column.setNullable(!isPrimaryKey);

            columns.add(column);
        }

        return columns;
    }

    @FXML
    private void addNewRecord() {
        if (currentTable == null) {
            showAlert("Ошибка", "Выберите таблицу для добавления записи");
            return;
        }

        try {
            org.example.entity.Table tableInfo = tableService.getTableInfo(currentTable);

            AddRecordDialog dialog = new AddRecordDialog(currentTable, tableInfo.getColumns());

            dialog.showAndWait().ifPresent(newData -> {
                if (newData != null && !newData.isEmpty()) {
                    insertRecordToDatabase(currentTable, newData);
                }
            });

        } catch (Exception e) {
            System.err.println("Error in addNewRecord: " + e.getMessage());
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось открыть форму добавления: " + e.getMessage());
        }
    }

    @FXML
    private void deleteSelectedRecord() {
        Map<String, Object> selectedRecord = tableView.getSelectionModel().getSelectedItem();

        if (selectedRecord == null) {
            showAlert("Ошибка", "Выберите запись для удаления");
            return;
        }

        if (currentTable == null) {
            showAlert("Ошибка", "Таблица не выбрана");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Удаление записи");
        confirmation.setContentText("Вы уверены, что хотите удалить выбранную запись?\nЭто действие нельзя отменить.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteRecordFromDatabase(currentTable, selectedRecord);
            }
        });
    }

    private void insertRecordToDatabase(String tableName, Map<String, Object> newData) {
        try {
            boolean success = tableService.insertData(tableName, newData);

            if (success) {
                showSuccessNotification("Запись успешно добавлена в базу данных");
                statusLabel.setText("Запись успешно добавлена");

                refreshTableData();
            } else {
                showAlert("Ошибка", "Не удалось добавить запись в базу данных");
            }

        } catch (Exception e) {
            handleDatabaseError(e);
        }
    }

    private void deleteRecordFromDatabase(String tableName, Map<String, Object> recordData) {
        try {
            boolean success = tableService.deleteData(tableName, recordData);

            if (success) {
                showSuccessNotification("Запись успешно удалена из базы данных");
                statusLabel.setText("Запись успешно удалена");

                refreshTableData();
            } else {
                showAlert("Ошибка", "Не удалось удалить запись из базы данных");
            }

        } catch (Exception e) {
            handleDatabaseError(e);
        }
    }

    private void updateRecordInDatabase(String tableName, Map<String, Object> oldData, Map<String, Object> newData) {
        try {
            String primaryKey = tableService.findPrimaryKeyColumn(tableName);
            if (primaryKey != null) {
                newData.remove(primaryKey);
            }

            boolean success = tableService.updateData(tableName, oldData, newData);

            if (success) {
                showSuccessNotification("Запись успешно обновлена в базе данных");
                statusLabel.setText("Запись успешно обновлена");

                refreshTableData();
            } else {
                showAlert("Ошибка", "Не удалось обновить запись");
            }

        } catch (Exception e) {
            handleDatabaseError(e);
        }
    }

    private void showSuccessNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Обновляем обработчик ошибок
    private void handleDatabaseError(Exception e) {
        String errorMessage = e.getMessage();

        if (errorMessage.contains("violates not-null constraint")) {
            showAlert("Ошибка валидации", "Нельзя установить NULL значение для обязательного поля");
        } else if (errorMessage.contains("violates foreign key constraint")) {
            showAlert("Ошибка валидации", "Нарушение целостности внешнего ключа");
        } else if (errorMessage.contains("violates unique constraint")) {
            showAlert("Ошибка валидации", "Нарушение уникальности данных");
        } else if (errorMessage.contains("value too long")) {
            showAlert("Ошибка валидации", "Значение слишком длинное для поля");
        } else if (errorMessage.contains("invalid input syntax")) {
            showAlert("Ошибка валидации", "Неверный формат данных для поля");
        } else if (errorMessage.contains("out of range")) {
            showAlert("Ошибка валидации", "Значение вне допустимого диапазона для поля");
        } else {
            showAlert("Ошибка базы данных", errorMessage);
        }

        statusLabel.setText("Ошибка операции: " + e.getMessage());
    }

    private void setupNavigation() {
        if (navigationService == null) return;

        navigationService.extendedModeProperty().addListener((obs, oldVal, newVal) -> {
            updateNavigationUI(newVal);
        });

        navPanel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (navigationService.isExtendedMode() &&
                            !navPanel.getBoundsInParent().contains(event.getX(), event.getY())) {
                        navigationService.collapseNavigation();
                    }
                });
            }
        });
    }

    @FXML
    private void toggleNavigationMode() {
        navigationService.toggleNavigationMode();
    }

    private void updateNavigationUI(boolean extendedMode) {
        if (extendedMode) {
            extendedToolbar.setVisible(true);
            extendedToolbar.setManaged(true);
            mainToolbar.setVisible(false);
            mainToolbar.setManaged(false);
        } else {
            mainToolbar.setVisible(true);
            mainToolbar.setManaged(true);
            extendedToolbar.setVisible(false);
            extendedToolbar.setManaged(false);
        }
    }

    private void setupQueryTab() {
        updateSavedQueriesCombo();

        savedQueriesCombo.setOnAction(e -> {
            String selectedQuery = savedQueriesCombo.getValue();
            if (selectedQuery != null) {
                queryService.getSavedQueries().stream()
                        .filter(q -> q.getName().equals(selectedQuery))
                        .findFirst()
                        .ifPresent(query -> {
                            queryTextArea.setText(query.getQuery());
                        });
            }
        });
    }

    @FXML
    private void createTable() {
        try {
            CreateTableDialog dialog = new CreateTableDialog();

            dialog.showAndWait().ifPresent(columns -> {
                if (columns != null && !columns.isEmpty()) {
                    String tableName = dialog.getTableName();
                    createTableInDatabase(tableName, columns);
                }
            });

        } catch (Exception e) {
            System.err.println("Error in createTable: " + e.getMessage());
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось открыть форму создания таблицы: " + e.getMessage());
        }
    }

    private void createTableInDatabase(String tableName, List<Column> columns) {
        try {
            boolean success = tableService.createTable(tableName, columns);

            if (success) {
                showSuccessNotification("Таблица '" + tableName + "' успешно создана");
                statusLabel.setText("Таблица '" + tableName + "' создана");

                refreshAfterTableOperation();
            } else {
                showAlert("Ошибка", "Не удалось создать таблицу '" + tableName + "'");
            }

        } catch (Exception e) {
            handleDatabaseError(e);
        }
    }

    @FXML
    private void dropTable() {
        String selectedTable = tablesList.getSelectionModel().getSelectedItem();
        if (selectedTable == null) {
            showAlert("Ошибка", "Выберите таблицу для удаления");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Удаление таблицы: " + selectedTable);
        confirmation.setContentText("Вы уверены, что хотите удалить таблицу '" + selectedTable + "'?\nЭто действие нельзя отменить.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = tableService.dropTable(selectedTable);
                    if (success) {
                        showSuccessNotification("Таблица '" + selectedTable + "' успешно удалена");
                        statusLabel.setText("Таблица '" + selectedTable + "' удалена");
//                        loadTablesForCurrentSchema();
                        refreshAfterTableOperation();
                    }
                } catch (Exception e) {
                    showAlert("Ошибка удаления таблицы", e.getMessage());
                }
            }
        });
    }

    private void refreshAfterTableOperation() {
        loadTablesForCurrentSchema();

        currentTable = null;
        if (currentTableLabel != null) {
            currentTableLabel.setText("Выберите таблицу для просмотра данных");
        }

        tableView.getItems().clear();
        tableView.getColumns().clear();

        updateButtonsState();
    }

    @FXML
    private void refreshTablesList() {
        loadTablesForCurrentSchema();
        statusLabel.setText("Список таблиц обновлен");
    }

    @FXML private void refreshTableData() {
        if (currentTable != null) {
            loadTablePreview(currentTable);
        }
    }

    @FXML
    private void createBackup() {
        try {
            List<String> tables = databaseService.getTablesInCurrentSchema();

            CreateBackupDialog dialog = new CreateBackupDialog(backupService, tables);

            dialog.showAndWait().ifPresent(result -> {
                if (result != null && result.isSuccess()) {
                    showSuccessNotification(result.getMessage());
                    statusLabel.setText("Бэкап создан: " + result.getBackupFile().getName());
                } else if (result != null) {
                    showAlert("Ошибка создания бэкапа", result.getMessage());
                }
            });

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось создать бэкап: " + e.getMessage());
        }
    }

    @FXML
    private void restoreBackup() {
        try {
            RestoreBackupDialog restoreDialog = new RestoreBackupDialog(backupService);

            restoreDialog.showAndWait().ifPresent(selectedBackup -> {
                if (selectedBackup != null) {
                    BackupConfirmationDialog confirmDialog = new BackupConfirmationDialog(selectedBackup);

                    confirmDialog.showAndWait().ifPresent(confirmed -> {
                        if (confirmed) {
                            performRestore(selectedBackup);
                        }
                    });
                }
            });

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось выполнить восстановление: " + e.getMessage());
        }
    }

    private void performRestore(File backupFile) {
        try {
            BackupService.BackupResult result = backupService.restoreBackup(backupFile);

            if (result.isSuccess()) {
                showSuccessNotification(result.getMessage());
                statusLabel.setText("Бэкап восстановлен");

                refreshAfterBackupRestore();
                if (currentTable != null) {
                    refreshTableData();
                }
            } else {
                showAlert("Ошибка восстановления", result.getMessage());
            }

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось восстановить бэкап: " + e.getMessage());
        }
    }

    private void refreshAfterBackupRestore() {
        loadSchemas();

        loadTablesForCurrentSchema();

        if (currentTable != null) {
            refreshTableData();
        }

        updateButtonsState();
    }

    @FXML
    private void executeQuery() {
        String sqlQuery = queryTextArea.getText().trim();
        if (sqlQuery.isEmpty()) {
            showAlert("Ошибка", "Введите SQL запрос");
            return;
        }

        try {
            QueryService.QueryResult result = queryService.executeQuery(sqlQuery);

            if (result.isSuccess()) {
                queryResultTable.getItems().clear();
                queryResultTable.getColumns().clear();

                if (!result.getColumns().isEmpty()) {
                    displayQueryResults(result);
                    showSuccessNotification("Запрос выполнен успешно. Найдено строк: " + result.getData().size());
                    statusLabel.setText("Запрос выполнен успешно. Найдено строк: " + result.getData().size());
                } else {
                    showSuccessNotification(result.getMessage());
                    statusLabel.setText(result.getMessage());
                }
            } else {
                showAlert("Ошибка выполнения запроса", result.getMessage());
                statusLabel.setText("Ошибка выполнения запроса");
            }
        } catch (Exception e) {
            showAlert("Критическая ошибка", "Неожиданная ошибка при выполнении запроса: " + e.getMessage());
            statusLabel.setText("Критическая ошибка выполнения запроса");
            e.printStackTrace(); // Для отладки
        }
    }

    private void displayQueryResults(QueryService.QueryResult result) {
        queryResultTable.getItems().clear();
        queryResultTable.getColumns().clear();

        for (String columnName : result.getColumns()) {
            TableColumn<Object, Object> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Map<String, Object> row = (Map<String, Object>) cellData.getValue();
                Object value = row.get(columnName);
                return new SimpleObjectProperty<>(value);
            });
            queryResultTable.getColumns().add(column);
        }

        queryResultTable.getItems().addAll((List<Object>) (List<?>) result.getData());
    }

    @FXML
    private void saveCurrentQuery() {
        String queryText = queryTextArea.getText().trim();
        if (queryText.isEmpty()) {
            showAlert("Ошибка", "Нет запроса для сохранения");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Сохранение запроса");
        dialog.setHeaderText("Введите название для запроса");
        dialog.setContentText("Название:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                queryService.saveQuery(name.trim(), queryText, "Сохраненный запрос");
                updateSavedQueriesCombo();
                statusLabel.setText("Запрос сохранен: " + name);
            }
        });
    }

    private void updateSavedQueriesCombo() {
        savedQueriesCombo.getItems().clear();
        queryService.getSavedQueries().forEach(query -> savedQueriesCombo.getItems().add(query.getName()));
    }

    @FXML
    private void deleteSavedQuery() {
        String selectedQuery = savedQueriesCombo.getValue();
        if (selectedQuery == null) {
            showAlert("Ошибка", "Выберите запрос для удаления");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Удаление запроса");
        confirmation.setContentText("Вы уверены, что хотите удалить запрос '" + selectedQuery + "'?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                queryService.deleteSavedQuery(selectedQuery);
                updateSavedQueriesCombo();
                savedQueriesCombo.getSelectionModel().clearSelection();
                statusLabel.setText("Запрос удален: " + selectedQuery);
            }
        });
    }

    @FXML
    private void clearQuery() {
        if (queryTextArea != null) {
            queryTextArea.clear();
        }
        if (queryResultTable != null) {
            queryResultTable.getItems().clear();
            queryResultTable.getColumns().clear();
        }
        if (savedQueriesCombo != null) {
            savedQueriesCombo.getSelectionModel().clearSelection();
        }
        statusLabel.setText("Очищено");
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
        statusLabel.setText("Ошибка: " + title);
    }

    @FXML
    private void exportCurrentSchema() {
        try {
            ExportService.ExportResult result = exportService.exportCurrentSchema();

            if (result.isSuccess()) {
                showSuccessNotification(result.getMessage());
                statusLabel.setText("Схема экспортирована: " + result.getExportFile().getName());
            } else {
                showAlert("Ошибка экспорта", result.getMessage());
            }

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось экспортировать схему: " + e.getMessage());
        }
    }

    @FXML
    private void exportCurrentTable() {
        if (currentTable == null) {
            showAlert("Ошибка", "Выберите таблицу для экспорта");
            return;
        }

        try {
            ExportService.ExportResult result = exportService.exportTable(currentTable);

            if (result.isSuccess()) {
                showSuccessNotification(result.getMessage());
                statusLabel.setText("Таблица экспортирована: " + result.getExportFile().getName());
            } else {
                showAlert("Ошибка экспорта", result.getMessage());
            }

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось экспортировать таблицу: " + e.getMessage());
        }
    }

    @FXML
    private void exportQueryResults() {
        if (queryResultTable.getItems().isEmpty()) {
            showAlert("Ошибка", "Нет результатов запроса для экспорта");
            return;
        }

        try {
            List<String> columns = new ArrayList<>();
            for (TableColumn<?, ?> column : queryResultTable.getColumns()) {
                columns.add(column.getText());
            }

            List<Map<String, Object>> data = new ArrayList<>();
            for (Object item : queryResultTable.getItems()) {
                if (item instanceof Map) {
                    data.add((Map<String, Object>) item);
                }
            }

            String queryName = "query_results";
            if (!queryTextArea.getText().trim().isEmpty()) {
                String queryText = queryTextArea.getText().trim();
                queryName = queryText.substring(0, Math.min(20, queryText.length())).replaceAll("[^a-zA-Z0-9]", "_");
            }

            ExportService.ExportResult result = exportService.exportQueryResults(queryName, columns, data);

            if (result.isSuccess()) {
                showSuccessNotification(result.getMessage());
                statusLabel.setText("Результаты экспортированы: " + result.getExportFile().getName());
            } else {
                showAlert("Ошибка экспорта", result.getMessage());
            }

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось экспортировать результаты: " + e.getMessage());
        }
    }
}
