package com.nirwan.dentalclinic.controllers.dialogs;

import com.nirwan.dentalclinic.models.Payment;
import com.nirwan.dentalclinic.models.Treatment;
import com.nirwan.dentalclinic.models.TreatmentCost;
import com.nirwan.dentalclinic.repository.TreatmentDao;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Window;

import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class TreatmentDetailsDialogController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label treatmentIdLabel;
    @FXML private Label statusLabel;
    @FXML private CheckBox activeCheckBox;
    @FXML private TextArea descriptionArea;
    @FXML private Label dateCreatedLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label amountPaidLabel;
    @FXML private Label amountPendingLabel;
    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, String> paymentDateCol;
    @FXML private TableColumn<Payment, Double> amountCol;
    @FXML private TableColumn<Payment, String> methodCol;
    @FXML private TableColumn<Payment, String> notesCol;
    @FXML private TableView<TreatmentCost> costHistoryTable;
    @FXML private TableColumn<TreatmentCost, String> effectiveDateCol;
    @FXML private TableColumn<TreatmentCost, Double> costCol;
    @FXML private TableColumn<TreatmentCost, String> costStatusCol;
    @FXML private TableColumn<TreatmentCost, String> costNotesCol;
    @FXML private Label errorLabel;
    @FXML private DialogPane dialogPane;
    
    private final TreatmentDao treatmentDao = new TreatmentDao();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
    private Treatment treatment;
    private boolean dataChanged = false;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupDialogButtons();
    }
    
    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
        updateUI();
    }
    
    private void setupTableColumns() {
        // Payment table columns
        paymentDateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        
        // Format amount column
        amountCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? "" : currencyFormat.format(amount));
                setStyle(amount > 0 ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "");
            }
        });
        
        // Cost history table columns
        effectiveDateCol.setCellValueFactory(new PropertyValueFactory<>("effectiveDate"));
        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        costStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        costNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        
        // Format cost column
        costCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double cost, boolean empty) {
                super.updateItem(cost, empty);
                setText(empty || cost == null ? "" : currencyFormat.format(cost));
            }
        });
    }
    
    private void setupDialogButtons() {
        // Set up the Done button
        Button doneButton = (Button) dialogPane.lookupButton(ButtonType.FINISH);
        doneButton.setText("Close");
        doneButton.setOnAction(event -> dialogPane.getScene().getWindow().hide());
    }
    
    private void updateUI() {
        if (treatment == null) return;
        
        // Update header
        titleLabel.setText("Treatment #" + treatment.getTreatmentId());
        subtitleLabel.setText("For: " + treatment.getPatientId()); // TODO: Replace with actual patient name
        
        // Update treatment details
        treatmentIdLabel.setText(treatment.getTreatmentId());
        activeCheckBox.setSelected(treatment.isActive());
        statusLabel.setText(treatment.isActive() ? "Active" : "Inactive");
        statusLabel.setStyle(treatment.isActive() ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "-fx-text-fill: #757575;");
        
        descriptionArea.setText(treatment.getDescription());
        dateCreatedLabel.setText(treatment.getCreatedAt().format(dateFormatter));
        lastUpdatedLabel.setText(treatment.getUpdatedAt().format(dateFormatter));
        
        // Update financial information
        totalCostLabel.setText(currencyFormat.format(treatment.getTotalAmount()));
        amountPaidLabel.setText(currencyFormat.format(treatment.getAmountPaid()));
        amountPendingLabel.setText(currencyFormat.format(treatment.getAmountPending()));
        
        // Load payment history
        loadPaymentHistory();
        
        // Load cost history
        loadCostHistory();
    }
    
    private void loadPaymentHistory() {
        // TODO: Implement method to load payment history from database
        // List<Payment> payments = treatmentDao.getPaymentsForTreatment(treatment.getId());
        // paymentsTable.getItems().setAll(payments);
    }
    
    private void loadCostHistory() {
        // TODO: Implement method to load cost history from database
        // List<TreatmentCost> costs = treatmentDao.getCostHistoryForTreatment(treatment.getId());
        // costHistoryTable.getItems().setAll(costs);
    }
    
    @FXML
    private void handleStatusChange() {
        if (treatment != null) {
            boolean newStatus = activeCheckBox.isSelected();
            treatment.setActive(newStatus);
            statusLabel.setText(newStatus ? "Active" : "Inactive");
            statusLabel.setStyle(newStatus ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "-fx-text-fill: #757575;");
            
            // Update in database
            treatmentDao.updateTreatment(treatment);
            dataChanged = true;
        }
    }
    
    @FXML
    private void handleAddPayment() {
        // TODO: Implement payment dialog
        // PaymentDialog dialog = new PaymentDialog(treatment);
        // Optional<Payment> result = dialog.showAndWait(dialogPane.getScene().getWindow());
        // result.ifPresent(payment -> {
        //     // Reload payment history
        //     loadPaymentHistory();
        //     // Update treatment summary
        //     updateUI();
        //     dataChanged = true;
        // });
    }
    
    public boolean showAndWait(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Treatment Details");
        
        // Set the dialog content
        dialog.setDialogPane(dialogPane);
        
        // Show the dialog and wait for response
        dialog.showAndWait();
        
        return dataChanged;
    }
}
