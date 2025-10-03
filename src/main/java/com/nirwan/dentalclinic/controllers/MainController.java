package com.nirwan.dentalclinic.controllers;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.models.PatientTreatmentDto;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.stage.Stage;
import com.nirwan.dentalclinic.repository.PatientDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class MainController {
    @FXML private TableView<PatientTreatmentDto> patientTable;
    @FXML private TableColumn<PatientTreatmentDto, Long> colPatientId;
    @FXML private TableColumn<PatientTreatmentDto, String> colPatientName;
    @FXML private TableColumn<PatientTreatmentDto, String> colTreatmentId;
    @FXML private TableColumn<PatientTreatmentDto, String> colDescription;
    @FXML private TableColumn<PatientTreatmentDto, Number> colTotalAmount;
    @FXML private TableColumn<PatientTreatmentDto, Number> colAmountPaid;
    @FXML private TableColumn<PatientTreatmentDto, Number> colAmountPending;
    @FXML private TableColumn<PatientTreatmentDto, String> colStatus;
    @FXML private TableColumn<PatientTreatmentDto, LocalDate> colTreatmentDate;
    @FXML private Button btnAddPatient;
    @FXML private Button btnAddTreatment;
    @FXML private Button btnRecordPayment;
    @FXML private Button btnExportData;

    private final ObservableList<PatientTreatmentDto> patientData = FXCollections.observableArrayList();
    private Stage primaryStage;

    /**
     * Sets the primary stage for this controller
     * @param primaryStage The primary stage of the application
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadPatientData();
        setupButtonActions();
    }

    private void setupTableColumns() {
        colPatientId.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getPatientId()).asObject());
        colPatientId.setVisible(false);
        // Set up cell value factories for each column
        colPatientName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        // Treatment date column with formatting
        colTreatmentDate.setCellValueFactory(cellData -> cellData.getValue().treatmentDateProperty());
        colTreatmentDate.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    setText(formatter.format(date));
                }
            }
        });
        
        // Description column
        colDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        
        // Amount columns with currency formatting
        colTotalAmount.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalAmount()));
        colTotalAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("₹%.2f", item.doubleValue()));
            }
        });

        colAmountPaid.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmountPaid()));
        colAmountPaid.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("₹%.2f", item.doubleValue()));
            }
        });

        colAmountPending.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmountPending()));
        colAmountPending.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("₹%.2f", item.doubleValue()));
            }
        });
        colAmountPending.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    double amount = item.doubleValue();
                    setText(String.format("₹%.2f", amount));
                    // Highlight pending amounts in red
                    if (amount > 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Status column with color coding
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PAID":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "PARTIALLY_PAID":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Numeric columns with currency formatting
        colTotalAmount.setCellValueFactory(cellData -> cellData.getValue().totalAmountProperty());
        colTotalAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("₹%.2f", item.doubleValue()));
            }
        });
        
        colAmountPaid.setCellValueFactory(cellData -> cellData.getValue().amountPaidProperty());
        colAmountPaid.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("₹%.2f", item.doubleValue()));
            }
        });
        
        colAmountPending.setCellValueFactory(cellData -> cellData.getValue().amountPendingProperty());
        colAmountPending.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    double amount = item.doubleValue();
                    setText(String.format("₹%.2f", amount));
                    // Optional: Highlight negative amounts in red
                    if (amount > 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Status column with color coding
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PAID":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "PARTIALLY_PAID":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }

    private void loadPatientData() {
        System.out.println("Loading patient data...");
        patientData.clear();
        // Query to get the latest treatment for each patient with the most recent treatment date
        String sql = "WITH LatestTreatment AS (\n" +
                   "    SELECT t.*, \n" +
                   "           ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY t.updated_at DESC) as rn,\n" +
                   "           (\n" +
                   "               SELECT status \n" +
                   "               FROM TreatmentCost tc \n" +
                   "               WHERE tc.treatment_id = t.treatment_id \n" +
                   "               ORDER BY tc.effective_from DESC \n" +
                   "               LIMIT 1\n" +
                   "           ) as status\n" +
                   "    FROM Patient p\n" +
                   "    JOIN Treatment t ON p.id = t.patient_id\n" +
                   "    WHERE p.is_deleted = FALSE AND t.is_deleted = FALSE\n" +
                   "),\n" +
                   "LatestTreatmentWithDate AS (\n" +
                   "    SELECT t.*, \n" +
                   "           (SELECT MAX(p.payment_date) FROM Payment p WHERE p.treatment_id = t.treatment_id) as last_payment_date,\n" +
                   "           t.updated_at as treatment_updated\n" +
                   "    FROM LatestTreatment t\n" +
                   "    WHERE t.rn = 1\n" +
                   ")\n" +
                   "SELECT p.id,p.name, t.treatment_id, t.description, t.total_amount, \n" +
                   "       t.amount_paid, t.amount_pending, t.status, \n" +
                   "       COALESCE(t.last_payment_date, t.treatment_updated) as treatment_date\n" +
                   "FROM Patient p\n" +
                   "Left JOIN LatestTreatmentWithDate t ON p.id = t.patient_id\n" +
                   "ORDER BY p.name";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                PatientTreatmentDto dto = new PatientTreatmentDto();
                dto.patientIdProperty().set(rs.getLong("id"));
                dto.nameProperty().set(rs.getString("name"));
                dto.treatmentIdProperty().set(rs.getString("treatment_id"));
                dto.descriptionProperty().set(rs.getString("description"));
                dto.totalAmountProperty().set(rs.getDouble("total_amount"));
                dto.amountPaidProperty().set(rs.getDouble("amount_paid"));
                dto.amountPendingProperty().set(rs.getDouble("amount_pending"));
                dto.statusProperty().set(rs.getString("status"));
                
                // Convert SQL date to LocalDate
                java.sql.Timestamp timestamp = rs.getTimestamp("treatment_date");
                if (timestamp != null) {
                    dto.treatmentDateProperty().set(timestamp.toLocalDateTime().toLocalDate());
                }
                
                patientData.add(dto);
            }
            patientTable.setItems(patientData);
        } catch (SQLException e) {
            showError("Database Error", "Error loading patient treatment data: " + e.getMessage());
        }
    }

    @FXML
    private void showAddPatientDialog() {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/views/patient-dialog.fxml"));
            Parent root = loader.load();
            
            // Create the dialog Stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Patient");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setScene(new Scene(root));
            
            // Set the dialog stage in the controller
            PatientDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            
            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();
            
            // Refresh the patient list if a patient was added
            if (controller.isSaveClicked()) {
                loadPatientData();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "Could not load the dialog: " + e.getMessage());
        }
    }
    
    private void setupButtonActions() {
        btnAddPatient.setOnAction(event -> showAddPatientDialog());
        btnAddTreatment.setOnAction(event -> showAddTreatmentDialog());
        btnRecordPayment.setOnAction(event -> showRecordPaymentDialog());
        
        // Add double-click handler to the patient table
        patientTable.setRowFactory(tv -> {
            TableRow<PatientTreatmentDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    PatientTreatmentDto selected = row.getItem();
                    openPatientView(selected);
                }
            });
            return row;
        });
    }
    
    private void openPatientView(PatientTreatmentDto patientDto) {
        try {
            // Load the patient view FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/views/patient-view.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the patient
            PatientViewController controller = loader.getController();
            
            // Fetch the complete patient data using patient ID
            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                PatientDao patientDao = new PatientDao();
                Long patientId = patientDto.getPatientId();
                if ( patientId != null) {
                    Optional<Patient> patientOpt = patientDao.findById(patientId);
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        controller.setPatient(patient);
                        
                        // Show patient view in the same window
                        Scene scene = primaryStage.getScene();
                        scene.setRoot(root);
                        primaryStage.setTitle("Patient Details - " + patient.getName());
                        primaryStage.sizeToScene();
                        return;
                    }
                }
                showError("Error", "Could not load patient details.");
            } catch (SQLException e) {
                showError("Database Error", "Error loading patient details: " + e.getMessage());
            }
        } catch (IOException e) {
            showError("Error", "Could not load patient view: " + e.getMessage());
        }
    }
    
    private String getPatientIdByTreatmentId(Connection conn, String treatmentId) throws SQLException {
        String sql = "SELECT patient_id FROM treatment WHERE treatment_id = ? AND is_deleted = false";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, treatmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("patient_id");
                }
            }
        }
        return null;
    }
    
    private void showAddTreatmentDialog() {
        PatientTreatmentDto selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a patient first.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/treatment-dialog.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the patient ID
//            TreatmentDialogController controller = loader.getController();
            // You'll need to implement setPatientId in the TreatmentDialogController
            // controller.setPatientId(selected.getPatientId());
            
            // Show the dialog
            Stage stage = new Stage();
            stage.setTitle("Add New Treatment");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            
            // Refresh data after dialog is closed
            stage.setOnHidden(e -> loadPatientData());
            stage.showAndWait();
            
        } catch (IOException e) {
            showError("Error", "Could not load the treatment dialog: " + e.getMessage());
        }
    }
    
    private void showRecordPaymentDialog() {
        PatientTreatmentDto selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a treatment to record payment for.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/payment-dialog.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the treatment ID and amount due
//            PaymentDialogController controller = loader.getController();
            // You'll need to implement these methods in the PaymentDialogController
            // controller.setTreatmentId(selected.getTreatmentId());
            // controller.setAmountDue(selected.getAmountPending());
            
            // Show the dialog
            Stage stage = new Stage();
            stage.setTitle("Record Payment");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            
            // Refresh data after dialog is closed
            stage.setOnHidden(e -> loadPatientData());
            stage.showAndWait();
            
        } catch (IOException e) {
            showError("Error", "Could not load the payment dialog: " + e.getMessage());
        }
    }

    private void exportToExcel() {
        // TODO: Implement Excel export functionality
        showInfo("Export to Excel", "Export to Excel functionality will be implemented here.");
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
