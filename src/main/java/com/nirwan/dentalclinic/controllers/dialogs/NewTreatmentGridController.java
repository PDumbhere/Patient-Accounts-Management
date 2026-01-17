package com.nirwan.dentalclinic.controllers.dialogs;

import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.models.Treatment;
import com.nirwan.dentalclinic.repository.TreatmentDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Optional;

public class NewTreatmentGridController {
    @FXML private ComboBox<String> treatment1Combo;
    @FXML private ComboBox<String> treatment2Combo;
    @FXML private ComboBox<String> treatment3Combo;
    @FXML private TextField costField;
    @FXML private TextField initialPaymentField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextArea notesField;
    @FXML private Label errorLabel;
    @FXML private DatePicker datePicker;
    @FXML private TextField treatment1Text;
    @FXML private TextField treatment2Text;
    @FXML private TextField treatment3Text;
    
    private final TreatmentDao treatmentDao = new TreatmentDao();
    private Patient patient;
    private Treatment createdTreatment;
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Initialize treatment combos with the same items
        initializeTreatmentCombo(treatment1Combo, true);
        initializeTreatmentCombo(treatment2Combo, false);
        initializeTreatmentCombo(treatment3Combo, false);

        // Add listeners to prevent duplicate selections
        setupTreatmentComboListeners();

        datePicker.setValue(LocalDate.now());
        // Payment methods
        paymentMethodCombo.getItems().addAll("Cash", "UPI");
        paymentMethodCombo.getSelectionModel().selectFirst();
    }

    private void setupTreatmentComboListeners() {
        // Listener for treatment1Combo
        treatment1Combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (newVal.equals(treatment2Combo.getValue())) {
                    treatment2Combo.setValue("");
                }
                if (newVal.equals(treatment3Combo.getValue())) {
                    treatment3Combo.setValue("");
                }
                if("Other".equalsIgnoreCase(newVal))
                    treatment1Text.setDisable(false);
                else
                    treatment1Text.setDisable(true);
            }
        });

        // Listener for treatment2Combo
        treatment2Combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (newVal.equals(treatment1Combo.getValue())) {
                    treatment1Combo.setValue("");
                }
                if (newVal.equals(treatment3Combo.getValue())) {
                    treatment3Combo.setValue("");
                }
                if("Other".equalsIgnoreCase(newVal))
                    treatment2Text.setDisable(false);
                else
                    treatment2Text.setDisable(true);
            }
        });

        // Listener for treatment3Combo
        treatment3Combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (newVal.equals(treatment1Combo.getValue())) {
                    treatment1Combo.setValue("");
                }
                if (newVal.equals(treatment2Combo.getValue())) {
                    treatment2Combo.setValue("");
                }
                if("Other".equalsIgnoreCase(newVal))
                    treatment3Text.setDisable(false);
                else
                    treatment3Text.setDisable(true);
            }
        });
    }

    private void initializeTreatmentCombo(ComboBox<String> combo, boolean isRequired) {
        if (combo == null) {
            combo = new ComboBox<>();
        }

        combo.getItems().setAll(
                "Consultation",
                "Extraction",
                "RCT",
                "Implant",
                "Ortho",
                "Prostho",
                "Cementation",
                "Scaling",
                "X-Ray",
                "FMR",
                "Bleaching",
                "Other"
        );

        combo.setEditable(false);

        if (isRequired) {
            combo.getSelectionModel().select(0); // Select first treatment for required field
        }
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
            dialogStage.getIcons().add(
                    new Image(NewTreatmentGridController.class.getResourceAsStream("/icons/icon.png"))
            );
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

    @FXML
    private void handleOk() {
        try {
            // Validate form
            if (!validateForm()) {
                return;
            }

            // Get form values
            String description = buildTreatmentDescription();
            double cost = Double.parseDouble(costField.getText());
            double initialPayment = initialPaymentField.getText().trim().isEmpty() ? 0 :
                    Double.parseDouble(initialPaymentField.getText().trim());
            String paymentMethod = paymentMethodCombo.getValue();
            String notes = notesField.getText().trim();
            LocalDate date = datePicker.getValue();

            // Create new treatment
            Treatment treatment = new Treatment();
            treatment.setTreatmentName(description);
            treatment.setTotalAmount(cost);
            treatment.setAmountPaid(initialPayment);
            treatment.setPaymentMethod(paymentMethod);
            treatment.setNotes(notes);
            treatment.setPatientId(patient.getId());
            treatment.setPaymentDate(date.atTime(LocalTime.now()));
            treatment.setActive(true);
            treatment.setCreatedAt(LocalDateTime.now());
            treatment.setUpdatedAt(LocalDateTime.now());

            // Save to database
            createdTreatment = treatmentDao.saveTreatment(treatment);

            if (createdTreatment == null) {
                errorLabel.setText("Failed to save treatment. Please try again.");
                return;
            }

            // Close the dialog
            if (dialogStage != null) {
                dialogStage.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error saving treatment: " + e.getMessage());
        }
    }

    
    private boolean validateForm() {
        // Validate first treatment is selected
        if (treatment1Combo.getValue() == null || treatment1Combo.getValue().trim().isEmpty()) {
            errorLabel.setText("Please select at least one treatment");
            return false;
        }
        
        // Validate cost
        if (costField.getText() == null || costField.getText().trim().isEmpty()) {
            errorLabel.setText("Please enter a cost");
            return false;
        }
        
        try {
            Double.parseDouble(costField.getText());
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid cost");
            return false;
        }
        
        // Validate initial payment if provided
        if (!initialPaymentField.getText().trim().isEmpty()) {
            try {
                Double.parseDouble(initialPaymentField.getText());
            } catch (NumberFormatException e) {
                errorLabel.setText("Please enter a valid initial payment");
                return false;
            }
        }
        
        return true;
    }
    
    private String buildTreatmentDescription() {
        StringBuilder description = new StringBuilder();
        
        // Add first treatment (required)
        if (treatment1Combo.getValue() != null && !treatment1Combo.getValue().trim().isEmpty()) {
            if("Other".equalsIgnoreCase(treatment1Combo.getValue().trim())){
                description.append(treatment1Text.getText().trim());
            }else{
                description.append(treatment1Combo.getValue().trim());
            }
        }
        
        // Add second treatment (optional)
        if (treatment2Combo.getValue() != null && !treatment2Combo.getValue().trim().isEmpty()) {
            if (description.length() > 0) description.append(" / ");
            if("Other".equalsIgnoreCase(treatment2Combo.getValue().trim())){
                description.append(treatment2Text.getText().trim());
            }else{
                description.append(treatment2Combo.getValue().trim());
            }
        }
        
        // Add third treatment (optional)
        if (treatment3Combo.getValue() != null && !treatment3Combo.getValue().trim().isEmpty()) {
            if (description.length() > 0) description.append(" / ");
            if("Other".equalsIgnoreCase(treatment3Combo.getValue().trim())){
                description.append(treatment3Text.getText().trim());
            }else{
                description.append(treatment3Combo.getValue().trim());
            }
        }
        
        return description.toString();
    }
    
    @FXML
    private void handleCancel() {
        createdTreatment = null;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    public void setPatient(Patient patient) {
        this.patient = patient;
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public Treatment getCreatedTreatment() {
        return createdTreatment;
    }
}