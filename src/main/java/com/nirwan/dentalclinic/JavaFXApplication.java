package com.nirwan.dentalclinic;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class JavaFXApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            System.out.println("Loading FXML...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/main-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // Get the controller and set the primary stage and main view root
            com.nirwan.dentalclinic.controllers.MainController controller = fxmlLoader.getController();
            controller.setPrimaryStage(stage);
            controller.setMainViewRoot(root);
            
            System.out.println("FXML loaded and controller initialized successfully");

            // Create scene
            Scene scene = new Scene(root, 900, 600);
            
            // Apply CSS styles
            String css = getClass().getResource("/styles/main.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
                System.out.println("CSS styles applied successfully");
            } else {
                System.err.println("Warning: Could not load CSS file");
            }
            
            stage.setTitle("Nirwan Dental Clinic - Account Management");
            stage.setScene(scene);
            stage.show();
            System.out.println("Stage shown successfully");

        } catch (Exception e) {
            System.err.println("Error in JavaFX Application start method:");
            e.printStackTrace();

            // Show error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText("An error occurred while starting the application:\n" + e.getMessage());
            alert.showAndWait();

            // Exit the application
            System.exit(1);
        }
    }
    @Override
    public void stop() throws Exception {
        super.stop();
        DatabaseConnection.getInstance().closeConnection();
        System.exit(0);
    }
}
