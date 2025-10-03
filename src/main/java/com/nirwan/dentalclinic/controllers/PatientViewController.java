package com.nirwan.dentalclinic.controllers;

import com.nirwan.dentalclinic.controllers.dialogs.NewTreatmentGridController;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.models.Treatment;
import com.nirwan.dentalclinic.repository.PatientDao;
import com.nirwan.dentalclinic.repository.TreatmentDao;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class PatientViewController implements Initializable {
    @FXML private Label patientNameLabel;
    @FXML private Label patientIdLabel;
    @FXML private Label patientStatusLabel;
    @FXML private Label phoneLabel;
    @FXML private Label emailLabel;
    @FXML private Label addressLabel;
    @FXML private Label bloodGroupLabel;
    @FXML private Label allergiesLabel;
    @FXML private Label medicalNotesLabel;
    @FXML private Label totalBalanceLabel;
    @FXML private Label lastUpdatedLabel;
    
    @FXML private TableView<Treatment> treatmentsTable;
    @FXML private TableColumn<Treatment, String> treatmentIdCol;
    @FXML private TableColumn<Treatment, String> descriptionCol;
    @FXML private TableColumn<Treatment, String> dateCol;
    @FXML private TableColumn<Treatment, Double> totalAmountCol;
    @FXML private TableColumn<Treatment, Double> paidAmountCol;
    @FXML private TableColumn<Treatment, Double> pendingAmountCol;
    @FXML private TableColumn<Treatment, Boolean> statusCol;
    
    private final TreatmentDao treatmentDao = new TreatmentDao();
    private final PatientDao patientDao = new PatientDao();
    private Patient currentPatient;
    private final ObservableList<Treatment> treatments = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        loadPatientData();
    }
    
    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        loadPatientData();
        loadTreatments();
    }
    
    private void setupTableColumns() {
        // Set up row factory for handling row clicks
        treatmentsTable.setRowFactory(tv -> {
            TableRow<Treatment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    handleTreatmentRowClick(event);
                }
            });
            return row;
        });
        
        // Set up cell value factories
        treatmentIdCol.setCellValueFactory(new PropertyValueFactory<>("treatmentId"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dateFormatter)));
        totalAmountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        paidAmountCol.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
        pendingAmountCol.setCellValueFactory(new PropertyValueFactory<>("amountPending"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        // Format currency columns
        totalAmountCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? "" : currencyFormat.format(amount));
            }
        });
        
        paidAmountCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? "" : currencyFormat.format(amount));
            }
        });
        
        pendingAmountCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                } else {
                    setText(currencyFormat.format(amount));
                    setStyle(amount > 0 ? "-fx-text-fill: #d32f2f; -fx-font-weight: bold;" : "");
                }
            }
        });
        
        // Format status column
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText("");
                } else {
                    setText(active ? "Active" : "Completed");
                    setStyle(active ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "-fx-text-fill: #757575;");
                }
            }
        });
    }
    
    private void loadPatientData() {
        if (currentPatient == null) return;
        
        patientNameLabel.setText(currentPatient.getName());
        patientIdLabel.setText(String.valueOf(currentPatient.getId()));
        patientStatusLabel.setText(!currentPatient.isDeleted() ? "Active" : "Inactive");
        
        lastUpdatedLabel.setText(currentPatient.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
    }
    
    private void loadTreatments() {
        if (currentPatient == null) return;
        
        treatments.setAll(treatmentDao.findByPatientId(currentPatient.getId()));
        treatmentsTable.setItems(treatments);
        updateTotalBalance();
    }
    
    private void updateTotalBalance() {
        double totalBalance = treatments.stream()
            .mapToDouble(Treatment::getAmountPending)
            .sum();
        totalBalanceLabel.setText(currencyFormat.format(totalBalance));
    }
    
    @FXML
    private void handleTreatmentRowClick(MouseEvent event) {
        if (event.getClickCount() == 2 && !treatmentsTable.getSelectionModel().isEmpty()) {
            Treatment selectedTreatment = treatmentsTable.getSelectionModel().getSelectedItem();
            showTreatmentDetails(selectedTreatment);
        }
    }

    @FXML
    private void handleNewTreatment() {
        try {
            // Load the FXML for the dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dialogs/new-treatment-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            // Create the dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("New Treatment");
            dialog.setDialogPane(dialogPane);

            // Show the dialog and wait for user input
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Get the controller to access the form data
                NewTreatmentGridController controller =
                        loader.getController();

                // Get the created treatment from the dialog
                Treatment savedTreatment = controller.getCreatedTreatment();
                if (savedTreatment != null) {
                    // The treatment is already saved in the dialog, just update the UI
                    treatments.add(savedTreatment);
                    updateTotalBalance();
                    loadTreatments(); // Refresh the table
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "Failed to load treatment dialog: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleEditPatient() {
        // TODO: Implement edit patient dialog
        // showEditPatientDialog();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleBackToList() {
        // Get the current stage (window) and close it
        Stage stage = (Stage) patientNameLabel.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void handleRefresh() {
        if (currentPatient != null) {
            loadPatientData();
            loadTreatments();
        }
    }
    
    private void showTreatmentDetails(Treatment treatment) {
        // TODO: Implement treatment details dialog
        // TreatmentDetailsDialog dialog = new TreatmentDetailsDialog(treatment);
        // dialog.showAndWait();
        // loadTreatments(); // Refresh treatment list if needed
    }
    
    @FXML
    private void showNewTreatmentDialog() {
        if (currentPatient == null) {
            showAlert("Error", "No patient selected", "Please select a patient first.", AlertType.ERROR);
            return;
        }

        try {
            // Show the new treatment dialog
            Optional<Treatment> result = NewTreatmentGridController.showDialog(currentPatient);
            
            // If treatment was saved successfully
            result.ifPresent(treatment -> {
                // Add the new treatment to the table
                treatmentsTable.getItems().add(treatment);
                updateTotalBalance();
                
                // Show success message
                showAlert("Success", "Treatment Added", 
                         "New treatment has been added successfully.", 
                         AlertType.INFORMATION);
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to add treatment", 
                     "An error occurred while adding the treatment: " + e.getMessage(), 
                     AlertType.ERROR);
        }
    }

    private void showAlert(String title, String header, String content, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
