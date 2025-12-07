package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.service.*;

import java.io.IOException;

public class WelcomeController {

    @FXML private Label dbNameLabel;
    @FXML private Button continueButton;

    private DatabaseService databaseService;
    private QueryService queryService;

    public void setDatabaseService(DatabaseService service) {
        this.databaseService = service;
        String dbName = service.getDatabaseName();
        dbNameLabel.setText("Database: " + dbName);
    }

    public void setQueryService(QueryService queryService) {
        this.queryService = queryService;
    }

    @FXML
    public void initialize() {
        continueButton.setOnAction(e -> openMainScreen());
    }

    public void openMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/layouts/main_layout.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();

            SchemaService schemaService = new SchemaService(databaseService);
            TableService tableService = new TableService(databaseService);
            NavigationService navService = new NavigationService();
            BackupService backupService = new BackupService(databaseService);
            ExportService exportService = new ExportService(databaseService);

            mainController.setServices(databaseService, schemaService, tableService, navService,
                    queryService, backupService, exportService);

            Stage stage = (Stage) continueButton.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("DB Manager - Main");

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setContentText("Не удалось загрузить главный экран: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
