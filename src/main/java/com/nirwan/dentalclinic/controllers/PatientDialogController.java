package com.nirwan.dentalclinic.controllers;

import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.repository.PatientDao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.SQLException;

public class PatientDialogController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;

    private Stage dialogStage;
    private boolean saveClicked = false;
    private Patient patient;
    private PatientDao patientDao;

    @FXML
    private void initialize() {
        // No initialization needed for VBox layout
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }
    
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleSave() {
        if (!isInputValid()) {
            return;
        }
        
        try {
            patient = new Patient();
            patient.setName(firstNameField.getText().trim()+" "+lastNameField.getText().trim());
            
            // Save to database
            patientDao = new PatientDao();
            boolean saved = patientDao.savePatient(patient);
            
            if (saved) {
                saveClicked = true;
                dialogStage.close();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Patient saved successfully!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save patient. Please try again.");
            }
        }  catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while saving patient: " + e.getMessage());
            return;
        }
    }
    
    // Removed getFullName() as we're using separate first/last name fields
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private boolean isInputValid() {
        String errorMessage = "";

        if (firstNameField.getText() == null || firstNameField.getText().trim().isEmpty()) {
            errorMessage += "First name is required!\n";
        }
        if (lastNameField.getText() == null || lastNameField.getText().trim().isEmpty()) {
            errorMessage += "Last name is required!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert(Alert.AlertType.ERROR, "Invalid Fields", "Please correct invalid fields", errorMessage);
            return false;
        }
    }
    

    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        if (!content.isEmpty()) {
            alert.setContentText(content);
        }
        alert.showAndWait();
    }

    public String getFirstName() {
        return firstNameField.getText().trim();
    }

    public String getLastName() {
        return lastNameField.getText().trim();
    }
}
