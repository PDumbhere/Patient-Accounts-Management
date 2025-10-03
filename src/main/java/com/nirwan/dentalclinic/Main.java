package com.nirwan.dentalclinic;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;



public class Main {
    public static void main(String[] args) {
        try {
            // Initialize the database first
            System.out.println("Initializing database...");
            DatabaseConnection.initializeDatabase();
            System.out.println("Database initialized successfully. Starting JavaFX application...");

            // If we get here, database is initialized, so start the JavaFX application
            Application.launch(JavaFXApplication.class, args);
        } catch (Exception e) {
            System.err.println("Failed to initialize the application:");
            e.printStackTrace();

            // Show error dialog if possible
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Startup Error");
                alert.setHeaderText("Failed to start application");
                alert.setContentText("An error occurred while starting the application: " + e.getMessage());
                alert.showAndWait();
            } catch (Exception ex) {
                // If we can't show the JavaFX alert, just print to console
                System.err.println("Could not show error dialog: " + ex.getMessage());
            }
            System.exit(1); // Exit with error code
        }
    }
}