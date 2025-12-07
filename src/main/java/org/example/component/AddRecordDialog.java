package org.example.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.example.entity.Column;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecordDialog extends Dialog<Map<String, Object>> {

    private final Map<String, TextField> fieldMap = new HashMap<>();
    private final List<Column> tableColumns;

    public AddRecordDialog(String tableName, List<Column> columns) {
        this.tableColumns = columns;

        setTitle("Добавление записи - " + tableName);
        setHeaderText("Введите данные для новой записи");

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

            if (!column.isNullable() && column.getDefaultValue() == null) {
                textField.setPromptText("Обязательное поле");
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: #cc0000; -fx-min-width: 120;");
            }

            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                textField.setPromptText("По умолчанию: " + column.getDefaultValue());
            }

            if (column.isPrimaryKey() && column.getType().toUpperCase().contains("SERIAL")) {
                textField.setDisable(true);
                textField.setText("авто");
                textField.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666;");
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc; -fx-min-width: 120;");
            }

            grid.add(label, 0, row);
            grid.add(textField, 1, row);

            fieldMap.put(column.getName(), textField);
            row++;
        }

        mainPanel.getChildren().add(grid);

        Label hintLabel = new Label("⚠️ Поля, выделенные синим цветом, заполняются автоматически\n⚠️ Поля, выделенные красным цветом, являются обязательными");
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
        mainPanel.getChildren().add(hintLabel);

        getDialogPane().setContent(mainPanel);

        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        setResultConverter(buttonType -> {
            if (buttonType == addButtonType) {
                return getFormData();
            }
            return null;
        });
    }

    private Map<String, Object> getFormData() {
        Map<String, Object> formData = new HashMap<>();

        for (Column column : tableColumns) {
            String columnName = column.getName();
            TextField field = fieldMap.get(columnName);

            if (field != null && !field.isDisable()) {
                String value = field.getText().trim();

                if (value.isEmpty()) {
                    // Для обязательных полей проверяем заполнение
                    if (!column.isNullable() && column.getDefaultValue() == null) {
                        showFieldError(columnName + " - обязательное поле");
                        return Collections.emptyMap();
                    }
                } else {
                    // Преобразуем значение в соответствующий тип
                    Object typedValue = convertValue(value, column);
                    formData.put(columnName, typedValue);
                }
            }
        }

        return formData;
    }

    private Object convertValue(String value, Column column) {
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
            showFieldError("Неверный формат данных для поля " + column.getName());
            throw e;
        }
    }

    private void showFieldError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка ввода");
        alert.setHeaderText("Проверьте введенные данные");
        alert.setContentText(message);
        alert.showAndWait();
    }
}