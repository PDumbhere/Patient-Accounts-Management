package com.nirwan.dentalclinic.controllers;

import com.nirwan.dentalclinic.models.User;
import com.nirwan.dentalclinic.repository.UserDao;
import com.nirwan.dentalclinic.security.SecurityUtils;
import com.nirwan.dentalclinic.security.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private Stage primaryStage;
    private UserDao userDao;

    @FXML
    public void initialize() {
        userDao = new UserDao();
        clearError();
        
        // Add enter key support for password field
        passwordField.setOnAction(event -> handleLogin());
        
        // Add focus listener to clear errors when user starts typing
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty()) {
            showError("Please enter username");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter password");
            return;
        }

        try {
            // Find user in database
            User user = userDao.findByUsername(username).orElse(null);
            
            if (user == null) {
                showError("Invalid username or password");
                return;
            }

            // Verify password
            if (!SecurityUtils.verifyPasswordHash(password, user.getPasswordHash())) {
                showError("Invalid username or password");
                return;
            }

            // Check if user is active
            if (!user.isActive()) {
                showError("Your account has been deactivated. Please contact administrator.");
                return;
            }

            // Update last login time
            userDao.updateLastLogin(user.getId());
            
            // Start session
            SessionManager.getInstance().startSession(user);
            
            // Load main application
            loadMainApplication();

        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMainApplication() {
        try {
            // Load main view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main-view.fxml"));
            Parent root = loader.load();
            
            // Get controller and set primary stage
            MainController mainController = loader.getController();
            mainController.setPrimaryStage(primaryStage);
            mainController.setMainViewRoot(root);
            
            // Set up main scene
            Scene scene = new Scene(root, 900, 600);
            
            // Apply CSS styles
            String css = getClass().getResource("/styles/main.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            // Configure stage
            primaryStage.setTitle("Nirwan Dental Clinic - Account Management");
            primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icons/icon.png"))
            );
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
        } catch (IOException e) {
            showError("Failed to load main application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText("Password Reset");
        alert.setContentText("Please contact your administrator to reset your password.");
        alert.showAndWait();
    }
}
