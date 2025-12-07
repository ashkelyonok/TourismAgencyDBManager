package org.example.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.io.File;

public class BackupConfirmationDialog extends Dialog<Boolean> {

    public BackupConfirmationDialog(File backupFile) {
        setTitle("Подтверждение восстановления");
        setHeaderText("Подтвердите восстановление базы данных");

        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().setMinWidth(450);

        ButtonType confirmButtonType = new ButtonType("Да, восстановить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

        VBox mainPanel = createContent(backupFile);
        getDialogPane().setContent(mainPanel);

        setResultConverter(buttonType -> {
            return buttonType == confirmButtonType;
        });
    }

    private VBox createContent(File backupFile) {
        VBox mainPanel = new VBox(15);
        mainPanel.setPadding(new Insets(15));

        Label messageLabel = new Label(
                "Вы уверены, что хотите восстановить базу данных из файла?\n\n" +
                        "Файл: " + backupFile.getName() + "\n\n" +
                        "⚠️ Это действие:\n" +
                        "• Перезапишет существующие данные\n" +
                        "• Может привести к потере текущих изменений\n" +
                        "• Не может быть отменено\n\n" +
                        "Рекомендуется создать бэкап текущего состояния перед восстановлением."
        );

        mainPanel.getChildren().add(messageLabel);

        return mainPanel;
    }
}