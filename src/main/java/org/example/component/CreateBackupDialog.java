package org.example.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.example.service.BackupService;

public class CreateBackupDialog extends Dialog<BackupService.BackupResult> {

    private ComboBox<String> backupTypeCombo;
    private ComboBox<String> tableCombo;

    public CreateBackupDialog(BackupService backupService, java.util.List<String> availableTables) {
        setTitle("Создание бэкапа");
        setHeaderText("Настройте параметры бэкапа");

        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().setMinWidth(400);

        ButtonType createButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(createButtonType, cancelButtonType);

        VBox mainPanel = createContent(availableTables);
        getDialogPane().setContent(mainPanel);

        setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return performBackup(backupService);
            }
            return null;
        });
    }

    private VBox createContent(java.util.List<String> availableTables) {
        VBox mainPanel = new VBox(15);
        mainPanel.setPadding(new Insets(15));

        GridPane typeGrid = new GridPane();
        typeGrid.setHgap(10);
        typeGrid.setVgap(10);

        Label typeLabel = new Label("Тип бэкапа:");
        typeLabel.setStyle("-fx-font-weight: bold;");

        backupTypeCombo = new ComboBox<>();
        backupTypeCombo.getItems().addAll("Полный бэкап схемы", "Бэкап одной таблицы");
        backupTypeCombo.setValue("Полный бэкап схемы");

        typeGrid.add(typeLabel, 0, 0);
        typeGrid.add(backupTypeCombo, 1, 0);

        Label tableLabel = new Label("Таблица:");
        tableLabel.setStyle("-fx-font-weight: bold;");
        tableLabel.setVisible(false);

        tableCombo = new ComboBox<>();
        tableCombo.getItems().addAll(availableTables);
        if (!availableTables.isEmpty()) {
            tableCombo.setValue(availableTables.get(0));
        }
        tableCombo.setVisible(false);

        typeGrid.add(tableLabel, 0, 1);
        typeGrid.add(tableCombo, 1, 1);

        backupTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isTableBackup = "Бэкап одной таблицы".equals(newVal);
            tableLabel.setVisible(isTableBackup);
            tableCombo.setVisible(isTableBackup);
        });

        Label infoLabel = new Label("Бэкап будет сохранен в папке backup/ в корне проекта");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        mainPanel.getChildren().addAll(typeGrid, infoLabel);

        return mainPanel;
    }

    private BackupService.BackupResult performBackup(BackupService backupService) {
        String backupType = backupTypeCombo.getValue();

        if ("Бэкап одной таблицы".equals(backupType)) {
            String selectedTable = tableCombo.getValue();
            if (selectedTable == null || selectedTable.isEmpty()) {
                showError("Выберите таблицу для бэкапа");
                return null;
            }
            return backupService.createTableBackup(selectedTable);
        } else {
            return backupService.createBackup();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Проверьте введенные данные");
        alert.setContentText(message);
        alert.showAndWait();
    }
}