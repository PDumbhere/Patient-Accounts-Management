package com.nirwan.dentalclinic.controllers;

import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.repository.PatientDao;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PatientDialogController {
    @FXML private Label titleLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;

    private Stage dialogStage;
    private boolean saveClicked = false;
    private Patient patient;
    private PatientDao patientDao;
    private boolean isEditMode = false;

    @FXML
    private void initialize() {
        // No initialization needed for VBox layout
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        this.isEditMode = (patient != null && patient.getId() > 0);

        if (isEditMode) {
            titleLabel.setText("Edit Patient");
            // Split the name into first and last name
            String[] nameParts = patient.getName().trim().split("\\s+", 2);
            if (nameParts.length >= 2) {
                firstNameField.setText(nameParts[0]);
                lastNameField.setText(nameParts[1]);
            } else if (nameParts.length == 1) {
                firstNameField.setText(nameParts[0]);
                lastNameField.setText("");
            }
        } else {
            titleLabel.setText("Add New Patient");
            firstNameField.clear();
            lastNameField.clear();
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public int getPatientId(){
        return patient.getId();
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
            String fullName = firstNameField.getText().trim() + " " + lastNameField.getText().trim();

            if (isEditMode) {
                // Update existing patient
                patient.setName(fullName);
                patientDao = new PatientDao();
                boolean updated = patientDao.updatePatient(patient);

                if (updated) {
                    saveClicked = true;
                    dialogStage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Patient updated successfully!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update patient. Please try again.");
                }
            } else {
                // Create new patient
                patient = new Patient();
                patient.setName(fullName);

                // Save to database
                patientDao = new PatientDao();
                int patientId = patientDao.savePatient(patient);

                if (patientId >0 ) {
                    saveClicked = true;
                    dialogStage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Patient saved successfully!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save patient. Please try again.");
                }
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

    public Patient getPatient() {
        return patient;
    }

    public String getFirstName() {
        return firstNameField.getText().trim();
    }

    public String getLastName() {
        return lastNameField.getText().trim();
    }
}