package org.example.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.example.entity.Column;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditRecordDialog extends Dialog<Map<String, Object>> {

    private final Map<String, TextField> fieldMap = new HashMap<>();
    private final List<Column> tableColumns;

    public EditRecordDialog(String tableName, Map<String, Object> recordData, List<Column> columns) {
        this.tableColumns = columns;

        setTitle("Редактирование записи - " + tableName);
        setHeaderText("Редактирование данных записи");

        initModality(Modality.APPLICATION_MODAL);

        VBox mainPanel = new VBox(10);
        mainPanel.setPadding(new Insets(15));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int row = 0;
        for (Column column : columns) {
            Label label = new Label(column.getName() + ":");
            label.setStyle("-fx-font-weight: bold; -fx-min-width: 120;");

            TextField textField = new TextField();
            textField.setPrefWidth(300);

            Object value = recordData.get(column.getName());
            if (value != null) {
                textField.setText(value.toString());
            }

            if (column.isPrimaryKey()) {
                textField.setDisable(true);
                textField.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666;");
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc; -fx-min-width: 120;");
            }

            grid.add(label, 0, row);
            grid.add(textField, 1, row);

            fieldMap.put(column.getName(), textField);
            row++;
        }

        mainPanel.getChildren().add(grid);

        Label hintLabel = new Label("⚠️ Поля, выделенные синим цветом, являются первичными ключами и не могут быть изменены");
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
        mainPanel.getChildren().add(hintLabel);

        getDialogPane().setContent(mainPanel);

        ButtonType applyButtonType = new ButtonType("Применить", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(applyButtonType, cancelButtonType);

        setResultConverter(buttonType -> {
            if (buttonType == applyButtonType) {
                return getUpdatedData();
            }
            return null;
        });
    }

    private Map<String, Object> getUpdatedData() {
        Map<String, Object> updatedData = new HashMap<>();

        for (Column column : tableColumns) {
            String columnName = column.getName();
            TextField field = fieldMap.get(columnName);

            if (field != null && !field.isDisable()) {
                String value = field.getText().trim();

                if (value.isEmpty()) {
                    updatedData.put(columnName, null);
                } else {
                    Object typedValue = convertValue(value, column);
                    updatedData.put(columnName, typedValue);
                }
            }
        }

        return updatedData;
    }

    private Object convertValue(String value, Column column) {
        if (value.isEmpty()) {
            return null;
        }

        String columnType = column.getType().toUpperCase();

        try {
            if (columnType.contains("INT") || columnType.contains("SERIAL")) {
                return Integer.parseInt(value);
            } else if (columnType.contains("DECIMAL") || columnType.contains("NUMERIC") ||
                    columnType.contains("FLOAT") || columnType.contains("REAL")) {
                return Double.parseDouble(value);
            } else if (columnType.contains("BOOLEAN") || columnType.contains("BOOL")) {
                return Boolean.parseBoolean(value) || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t");
            }
            return value;
        } catch (NumberFormatException e) {
            return value;
        }
    }
}