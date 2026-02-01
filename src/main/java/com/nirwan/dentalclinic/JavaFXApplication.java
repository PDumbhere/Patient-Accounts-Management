package com.nirwan.dentalclinic;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.security.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class JavaFXApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            // Load session from file to check if user is logged in today
            SessionManager sessionManager = SessionManager.getInstance();
            sessionManager.loadSessionFromFile();
            
            // Check if user needs to login (not logged in or new day)
            if (!sessionManager.isLoggedIn() || sessionManager.requiresNewLogin()) {
                showLoginScreen(stage);
            } else {
                showMainApplication(stage);
            }
            
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
    
    private void showLoginScreen(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/login-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // Get the controller and set the primary stage
            com.nirwan.dentalclinic.controllers.LoginController controller = fxmlLoader.getController();
            controller.setPrimaryStage(stage);
            
            // Create scene
            Scene scene = new Scene(root, 400, 500);
            
            // Apply login CSS styles
            String loginCss = getClass().getResource("/styles/login.css").toExternalForm();
            if (loginCss != null) {
                scene.getStylesheets().add(loginCss);
            }
            
            stage.setTitle("Nirwan Dental Clinic - Login");
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/icon.png"))
            );
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load login screen", e);
        }
    }
    
    private void showMainApplication(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/main-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // Get the controller and set the primary stage and main view root
            com.nirwan.dentalclinic.controllers.MainController controller = fxmlLoader.getController();
            controller.setPrimaryStage(stage);
            controller.setMainViewRoot(root);
            
            // Create scene
            Scene scene = new Scene(root, 900, 600);
            
            // Apply CSS styles
            String css = getClass().getResource("/styles/main.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            stage.setTitle("Nirwan Dental Clinic - Account Management");
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/icon.png"))
            );
            stage.setScene(scene);
            stage.show();
            
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
