package org.example.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.example.entity.Column;

import java.util.ArrayList;
import java.util.List;

public class CreateTableDialog extends Dialog<List<Column>> {

    private TextField tableNameField;
    private ScrollPane scrollPane;
    private VBox columnsPanel;
    private List<ColumnField> columnFields = new ArrayList<>();

    public CreateTableDialog() {
        setTitle("Создание таблицы");
        setHeaderText("Введите параметры новой таблицы");

        initModality(Modality.APPLICATION_MODAL);

        getDialogPane().setMinWidth(600);
        getDialogPane().setMinHeight(400);

        ButtonType createButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(createButtonType, cancelButtonType);

        VBox mainPanel = createContent();
        getDialogPane().setContent(mainPanel);

        setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return getTableColumns();
            }
            return null;
        });
    }

    private VBox createContent() {
        VBox mainPanel = new VBox(15);
        mainPanel.setPadding(new Insets(15));

        GridPane tableNameGrid = new GridPane();
        tableNameGrid.setHgap(10);
        tableNameGrid.setVgap(10);

        Label tableNameLabel = new Label("Имя таблицы:");
        tableNameLabel.setStyle("-fx-font-weight: bold;");
        tableNameField = new TextField();
        tableNameField.setPrefWidth(250);
        tableNameField.setText("new_table");

        tableNameGrid.add(tableNameLabel, 0, 0);
        tableNameGrid.add(tableNameField, 1, 0);

        Label columnsLabel = new Label("Колонки таблицы:");
        columnsLabel.setStyle("-fx-font-weight: bold;");

        columnsPanel = new VBox(10);
        columnsPanel.setPadding(new Insets(10));
        columnsPanel.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5;");

        scrollPane = new ScrollPane();
        scrollPane.setContent(columnsPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(200);
        scrollPane.setStyle("-fx-border-color: #ccc; -fx-border-radius: 3;");

        addDefaultIdColumn();

        Button addColumnButton = new Button("+ Добавить колонку");
        addColumnButton.setOnAction(e -> addColumnField());

        mainPanel.getChildren().addAll(tableNameGrid, columnsLabel, scrollPane, addColumnButton);

        return mainPanel;
    }

    private void addDefaultIdColumn() {
        ColumnField idField = new ColumnField();
        idField.nameField.setText("id");
        idField.typeCombo.setValue("SERIAL");
        idField.primaryKeyCheck.setSelected(true);
        idField.nullableCheck.setSelected(false);
        idField.nullableCheck.setDisable(true);
        idField.removeButton.setDisable(true);

        columnsPanel.getChildren().add(idField.getNode());
        columnFields.add(idField);
    }

    private void addColumnField() {
        ColumnField columnField = new ColumnField();
        columnsPanel.getChildren().add(columnField.getNode());
        columnFields.add(columnField);

        scrollPane.setVvalue(1.0);
    }

    private void removeColumnField(ColumnField columnField) {
        columnFields.remove(columnField);
        columnsPanel.getChildren().remove(columnField.getNode());
    }

    private List<Column> getTableColumns() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            showError("Введите имя таблицы");
            return null;
        }

        if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            showError("Имя таблицы может содержать только буквы, цифры и символ подчеркивания, и должно начинаться с буквы");
            return null;
        }

        List<Column> columns = new ArrayList<>();

        for (ColumnField field : columnFields) {
            String columnName = field.nameField.getText().trim();
            String columnType = field.typeCombo.getValue();

            if (columnName.isEmpty()) {
                showError("Введите имя для всех колонок");
                return null;
            }

            if (!columnName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                showError("Имя колонки '" + columnName + "' может содержать только буквы, цифры и символ подчеркивания, и должно начинаться с буквы");
                return null;
            }

            Column column = new Column(columnName, columnType);
            column.setPrimaryKey(field.primaryKeyCheck.isSelected());
            column.setNullable(field.nullableCheck.isSelected());

            if (!field.defaultValueField.getText().trim().isEmpty()) {
                column.setDefaultValue(field.defaultValueField.getText().trim());
            }

            columns.add(column);
        }

        return columns;
    }

    public String getTableName() {
        return tableNameField.getText().trim();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка ввода");
        alert.setHeaderText("Проверьте введенные данные");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class ColumnField {
        TextField nameField = new TextField();
        ComboBox<String> typeCombo = new ComboBox<>();
        CheckBox primaryKeyCheck = new CheckBox("PK");
        CheckBox nullableCheck = new CheckBox("NULL");
        TextField defaultValueField = new TextField();
        Button removeButton = new Button("✕");
        GridPane grid;

        ColumnField() {
            typeCombo.getItems().addAll(
                    "SERIAL", "INTEGER", "BIGINT", "SMALLINT",
                    "VARCHAR(255)", "TEXT", "BOOLEAN",
                    "DATE", "TIMESTAMP", "NUMERIC(10,2)"
            );
            typeCombo.setValue("VARCHAR(255)");

            typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if ("SERIAL".equals(newVal)) {
                    primaryKeyCheck.setSelected(true);
                    nullableCheck.setSelected(false);
                    nullableCheck.setDisable(true);
                } else {
                    nullableCheck.setDisable(false);
                }
            });

            primaryKeyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    nullableCheck.setSelected(false);
                    nullableCheck.setDisable(true);
                } else {
                    nullableCheck.setDisable(false);
                }
            });

            defaultValueField.setPromptText("Значение по умолчанию");
            removeButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-min-width: 30;");

            removeButton.setOnAction(e -> removeColumnField(this));
        }

        javafx.scene.Node getNode() {
            grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(5);
            grid.setPadding(new Insets(8));
            grid.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 3; -fx-background-color: #f9f9f9;");

            nameField.setPromptText("Имя колонки");
            nameField.setPrefWidth(120);
            typeCombo.setPrefWidth(120);
            defaultValueField.setPrefWidth(120);

            grid.add(nameField, 0, 0);
            grid.add(typeCombo, 1, 0);
            grid.add(primaryKeyCheck, 2, 0);
            grid.add(nullableCheck, 3, 0);
            grid.add(defaultValueField, 4, 0);
            grid.add(removeButton, 5, 0);

            return grid;
        }
    }
}