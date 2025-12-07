package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Getter;
import org.example.controller.WelcomeController;
import org.example.service.*;

@Getter
public class App extends Application {
    private DatabaseService databaseService;
    private SchemaService schemaService;
    private TableService tableService;
    private NavigationService navigationService;
    private QueryService queryService;
    private BackupService backupService;
    private ExportService exportService;

    @Override
    public void start(Stage primaryStage) {
        try {
            databaseService = new DatabaseService();
            schemaService = new SchemaService(databaseService);
            tableService = new TableService(databaseService);
            navigationService = new NavigationService();
            queryService = new QueryService(databaseService);
            backupService = new BackupService(databaseService);
            exportService = new ExportService(databaseService);


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/layouts/welcome.fxml"));
            Parent root = loader.load();

            WelcomeController controller = loader.getController();
            controller.setDatabaseService(databaseService);
            controller.setQueryService(queryService);

            Scene scene = new Scene(root, 700, 420);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            primaryStage.setTitle("DB Manager — Welcome");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            showErrorAlert("Application startup error",
                    "Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (databaseService != null) {
            databaseService.close();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void run(String[] args) {
        launch(args);
    }
}
