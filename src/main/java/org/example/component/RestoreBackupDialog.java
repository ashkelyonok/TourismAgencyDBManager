package org.example.component;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.example.service.BackupService;

import java.io.File;
import java.util.List;

public class RestoreBackupDialog extends Dialog<File> {

    private ListView<File> backupsList;

    public RestoreBackupDialog(BackupService backupService) {
        setTitle("Восстановление из бэкапа");
        setHeaderText("Выберите файл бэкапа для восстановления");

        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().setMinWidth(500);
        getDialogPane().setMinHeight(400);

        ButtonType restoreButtonType = new ButtonType("Восстановить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(restoreButtonType, cancelButtonType);

        VBox mainPanel = createContent(backupService);
        getDialogPane().setContent(mainPanel);

        setResultConverter(buttonType -> {
            if (buttonType == restoreButtonType) {
                return backupsList.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        Button restoreButton = (Button) getDialogPane().lookupButton(restoreButtonType);
        restoreButton.setDisable(true);

        backupsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> restoreButton.setDisable(newVal == null)
        );
    }

    private VBox createContent(BackupService backupService) {
        VBox mainPanel = new VBox(15);
        mainPanel.setPadding(new Insets(15));

        Label listLabel = new Label("Доступные бэкапы:");
        listLabel.setStyle("-fx-font-weight: bold;");

        backupsList = new ListView<>();
        backupsList.setPrefHeight(300);

        List<File> backups = backupService.getAvailableBackups();
        backupsList.setItems(FXCollections.observableArrayList(backups));

        Label selectedInfoLabel = new Label();
        selectedInfoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        backupsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectedInfoLabel.setText(
                                String.format("Файл: %s\nРазмер: %.2f KB\nИзменен: %s",
                                        newVal.getName(),
                                        newVal.length() / 1024.0,
                                        new java.util.Date(newVal.lastModified())
                                )
                        );
                    } else {
                        selectedInfoLabel.setText("");
                    }
                }
        );

        Label warningLabel = new Label("⚠️ Внимание: восстановление перезапишет существующие данные!");
        warningLabel.setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold;");

        mainPanel.getChildren().addAll(listLabel, backupsList, selectedInfoLabel, warningLabel);

        return mainPanel;
    }
}