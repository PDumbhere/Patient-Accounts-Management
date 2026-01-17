package com.nirwan.dentalclinic.controllers;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.Patient;
import com.nirwan.dentalclinic.models.PatientTreatmentDto;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.nirwan.dentalclinic.repository.PatientDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
    @FXML private Button btnExportData;
    @FXML private TextField searchField;

    private final ObservableList<PatientTreatmentDto> patientData = FXCollections.observableArrayList();
    private Stage primaryStage;
    private Parent mainViewRoot;
    private FilteredList<PatientTreatmentDto> filtered;
    private SortedList<PatientTreatmentDto> sorted;

    /**
     * Sets the primary stage for this controller
     * @param primaryStage The primary stage of the application
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    /**
     * Sets the main view root for navigation
     * @param root The root node of the main view
     */
    public void setMainViewRoot(Parent root) {
        this.mainViewRoot = root;
    }

    /**
     * Safely gets the current Stage by deriving it from any available control.
     */
    private Stage getStage() {
        // Try from patient table
        if (patientTable != null && patientTable.getScene() != null) {
            return (Stage) patientTable.getScene().getWindow();
        }
        // Try from buttons
        if (btnAddPatient != null && btnAddPatient.getScene() != null) {
            return (Stage) btnAddPatient.getScene().getWindow();
        }
        if (btnExportData != null && btnExportData.getScene() != null) {
            return (Stage) btnExportData.getScene().getWindow();
        }
        // Fallback to primaryStage if available
        return primaryStage;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFiltering();
        loadPatientData();
        setupButtonActions();
    }

    private void setupFiltering() {
        // Initialize filtered and sorted lists
        filtered = new FilteredList<>(patientData, dto -> true);
        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        patientTable.setItems(sorted);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                final String q = newV == null ? "" : newV.trim().toLowerCase();
                filtered.setPredicate(dto -> {
                    if (q.isEmpty()) return true;
                    String name = dto.getName() != null ? dto.getName().toLowerCase() : "";
                    return name.contains(q);
                });
            });
        }
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
        String sql = """
            WITH LatestTreatment AS (
                SELECT t.*,
                       ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY t.updated_at DESC) as rn,
                       (
                           SELECT status
                           FROM TreatmentCost tc
                           WHERE tc.treatment_id = t.treatment_id
                           ORDER BY tc.effective_from DESC
                           LIMIT 1
                       ) as status
                FROM Patient p
                JOIN Treatment t ON p.id = t.patient_id
                WHERE p.is_deleted = FALSE AND t.is_deleted = FALSE
            ),
            LatestTreatmentWithDate AS (
                SELECT t.*,
                       (SELECT MAX(p.payment_date) FROM Payment p WHERE p.treatment_id = t.treatment_id) as last_payment_date,
                       t.updated_at as treatment_updated
                FROM LatestTreatment t
                WHERE t.rn = 1
            )
            SELECT p.id,
                   p.name,
                   t.treatment_id,
                   t.treatment_name,
                   t.total_amount,
                   t.amount_paid,
                   t.amount_pending,
                   t.status,
                   COALESCE(t.last_payment_date, t.treatment_updated) as treatment_date
            FROM Patient p
            LEFT JOIN LatestTreatmentWithDate t ON p.id = t.patient_id
            ORDER BY p.name
            """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                PatientTreatmentDto dto = new PatientTreatmentDto();
                dto.patientIdProperty().set(rs.getLong("id"));
                dto.nameProperty().set(rs.getString("name"));
                dto.treatmentIdProperty().set(rs.getString("treatment_id"));
                dto.descriptionProperty().set(rs.getString("treatment_name"));
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
            // Items already set to sorted list; just refresh backing list
            // patientTable.setItems(sorted) was set in setupFiltering()
        } catch (SQLException e) {
            showError("Database Error", "Error loading patient treatment data: " + e.getMessage());
        }
    }

    @FXML
    private void clearSearch() {
        if (searchField != null) {
            searchField.clear();
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
            dialogStage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/icon.png"))
            );
            dialogStage.initModality(Modality.WINDOW_MODAL);
            // Derive owner from an existing node to avoid null primaryStage
            Stage ownerStage = (Stage) patientTable.getScene().getWindow();
            dialogStage.initOwner(ownerStage);
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
        if (btnAddPatient != null) {
            btnAddPatient.setOnAction(event -> showAddPatientDialog());
        }
        
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
                        
                        // Derive the stage from an existing control instead of relying on primaryStage
                        Stage stage = (Stage) patientTable.getScene().getWindow();
                        Scene currentScene = stage.getScene();
                        if (currentScene == null) {
                            currentScene = new Scene(root);
                            stage.setScene(currentScene);
                        } else {
                            currentScene.setRoot(root);
                        }
                        stage.setTitle("Patient Details - " + patient.getName());
                        stage.sizeToScene();
                        stage.getIcons().add(
                                new Image(getClass().getResourceAsStream("/icons/icon.png"))
                        );
                        stage.centerOnScreen();
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
    
    @FXML
    private void openPaymentsReport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/payments-report.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Payments Report");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/icon.png"))
            );
            Stage owner = getStage();
            if (owner != null) stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            // Keep width same as main view and position near it
            if (owner != null) {
                double w = owner.getWidth();
                if (w > 0) stage.setWidth(w);
                // place slightly offset within screen bounds
                stage.setX(owner.getX());
                stage.setY(owner.getY() + 30);
            }
            stage.showAndWait();
        } catch (IOException ex) {
            showError("Error", "Could not open Payments Report: " + ex.getMessage());
        }
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
