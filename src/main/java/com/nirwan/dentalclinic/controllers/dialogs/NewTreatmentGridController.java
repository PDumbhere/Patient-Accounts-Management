package com.nirwan.dentalclinic.controllers.dialogs;

import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.models.Treatment;
import com.nirwan.dentalclinic.repository.TreatmentDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Optional;

public class NewTreatmentGridController {
    @FXML private ComboBox<String> treatmentCombo;
    @FXML private TextField costField;
    @FXML private TextField initialPaymentField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextArea notesField;
    @FXML private Label errorLabel;

    private final TreatmentDao treatmentDao = new TreatmentDao();
    private Patient patient;
    private Treatment createdTreatment;
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Build or populate treatment combo (if not injected by FXML, create and replace TextArea)
        if (treatmentCombo == null) {
            treatmentCombo = new ComboBox<>();
        }
        treatmentCombo.getItems().setAll(
                "Consultation",
                "Extraction",
                "Root Canal Treatment",
                "Implant",
                "Ortho",
                "Prostho",
                "Cementation",
                "Scaling",
                "X-Ray"
        );
        treatmentCombo.setEditable(false);
        if (treatmentCombo.getSelectionModel().isEmpty()) {
            treatmentCombo.getSelectionModel().selectFirst();
        }

//        // Replace TextArea only if it exists (runtime swap path)
//        if (descriptionField != null && treatmentCombo.getParent() == null && descriptionField.getParent() instanceof GridPane grid) {
//            int idx = grid.getChildren().indexOf(descriptionField);
//            GridPane.setColumnIndex(treatmentCombo, 1);
//            GridPane.setRowIndex(treatmentCombo, 1);
//            GridPane.setHgrow(treatmentCombo, Priority.ALWAYS);
//            if (idx >= 0) {
//                grid.getChildren().set(idx, treatmentCombo);
//            } else {
//                grid.getChildren().remove(descriptionField);
//                grid.add(treatmentCombo, 1, 1);
//            }
//        }

        // Payment methods
        paymentMethodCombo.getItems().addAll("Cash", "Credit Card", "Debit Card", "UPI", "Insurance");
        paymentMethodCombo.getSelectionModel().selectFirst();
    }

    public static Optional<Treatment> showDialog(Patient patient) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NewTreatmentGridController.class.getResource("/views/dialogs/new-treatment-grid.fxml")
            );
            GridPane root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("New Treatment");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.DECORATED);
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            NewTreatmentGridController controller = loader.getController();
            controller.setPatient(patient);
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            return Optional.ofNullable(controller.getCreatedTreatment());

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Treatment getCreatedTreatment() {
        return createdTreatment;
    }

    @FXML
    private void handleOk() {
        createdTreatment = saveTreatment();
        if (createdTreatment != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        createdTreatment = null;
        dialogStage.close();
    }

    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (treatmentCombo == null || treatmentCombo.getValue() == null || treatmentCombo.getValue().trim().isEmpty()) {
            errorMessage.append("Treatment is required.\n");
        }
        
        try {
            if (costField.getText() == null || costField.getText().trim().isEmpty()) {
                errorMessage.append("Cost is required.\n");
            } else {
                double cost = Double.parseDouble(costField.getText());
                if (cost <= 0) {
                    errorMessage.append("Cost must be greater than 0.\n");
                }
            }
            
            if (!initialPaymentField.getText().isEmpty()) {
                double initialPayment = Double.parseDouble(initialPaymentField.getText());
                if (initialPayment < 0) {
                    errorMessage.append("Initial payment cannot be negative.\n");
                }
            }
            
        } catch (NumberFormatException e) {
            errorMessage.append("Please enter valid numbers for cost and payment fields.\n");
        }
        
        if (paymentMethodCombo.getValue() == null || paymentMethodCombo.getValue().isEmpty()) {
            errorMessage.append("Please select a payment method.\n");
        }
        
        if (errorMessage.length() > 0) {
            errorLabel.setText(errorMessage.toString());
            errorLabel.setVisible(true);
            return false;
        }
        
        errorLabel.setVisible(false);
        return true;
    }
    
    private Treatment saveTreatment() {
        try {
            // Validate form first
            if (!validateForm()) {
                return null;
            }
            
            // Get form values
            String description = treatmentCombo != null && treatmentCombo.getValue() != null
                    ? treatmentCombo.getValue().trim()
                    : "";
            double cost = Double.parseDouble(costField.getText());
            double initialPayment = initialPaymentField.getText().isEmpty() ? 0 : 
                                 Double.parseDouble(initialPaymentField.getText());
            String paymentMethod = paymentMethodCombo.getValue();
            String notes = notesField.getText().trim();
            
            // Create new treatment
            Treatment treatment = new Treatment(null, patient.getId(), description, cost);
            treatment.setAmountPaid(initialPayment);
            treatment.setAmountPending(cost - initialPayment);
            treatment.setPaymentMethod(paymentMethod);
            treatment.setNotes(notes);
            
            // Save treatment to database
            Treatment savedTreatment = treatmentDao.saveTreatment(treatment);
            
            if (savedTreatment == null) {
                errorLabel.setText("Failed to save treatment. Please try again.");
                return null;
            }
            
            return savedTreatment;
            
        } catch (Exception e) {
            errorLabel.setText("Error saving treatment: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}