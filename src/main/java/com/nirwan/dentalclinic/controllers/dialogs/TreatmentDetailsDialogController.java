package com.nirwan.dentalclinic.controllers.dialogs;

import com.nirwan.dentalclinic.models.Payment;
import com.nirwan.dentalclinic.models.Treatment;
import com.nirwan.dentalclinic.models.TreatmentCost;
import com.nirwan.dentalclinic.repository.TreatmentDao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Window;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

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
    @FXML private Button markCompletedBtn;
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
    // fx:id in FXML is "statusCol"; align the controller field name to avoid NPE
    @FXML private TableColumn<TreatmentCost, String> statusCol;
    @FXML private TableColumn<TreatmentCost, String> costNotesCol;
    @FXML private Label errorLabel;
    @FXML private DialogPane dialogPane;
    @FXML private Button deletePaymentBtn;
    @FXML private Button addPaymentBtn;
    @FXML private Button updateCostBtn;
    
    private final TreatmentDao treatmentDao = new TreatmentDao();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
    private Treatment treatment;
    private boolean dataChanged = false;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupDialogButtons();
        // Enable/disable delete button based on selection
        if (paymentsTable != null) {
            paymentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (deletePaymentBtn != null) {
                    deletePaymentBtn.setDisable(newSel == null);
                }
            });
        }
    }

    @FXML
    private void handleUpdateCost() {
        if (treatment == null || !treatment.isActive()) {
            errorLabel.setText("Cannot update cost on a completed treatment.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null);
        dialog.setTitle("Update Treatment Cost");

        DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField costField = new TextField();
        costField.setPromptText("New total cost");
        costField.setText(String.format("%.2f", treatment.getTotalAmount()));
        costField.setTextFormatter(new TextFormatter<>(c -> {
            if (c.getControlNewText().matches("^\\d*(\\.\\d{0,2})?$")) return c;
            return null;
        }));

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes about this change (optional)");
        notesArea.setPrefRowCount(3);

        grid.addRow(0, new Label("New Cost:"), costField);
        grid.addRow(1, new Label("Notes:"), notesArea);

        pane.setContent(grid);
        dialog.setDialogPane(pane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String text = costField.getText();
                double newCost = (text == null || text.isBlank()) ? treatment.getTotalAmount() : Double.parseDouble(text);
                if (newCost <= 0) {
                    errorLabel.setText("Cost must be greater than 0.");
                    return;
                }

                String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

                boolean ok = treatmentDao.addTreatmentCost(treatment, newCost, notes.isEmpty() ? "Cost updated" : notes);
                if (ok) {
                    // Update local model and UI
                    treatment.updateCost(newCost);
                    dataChanged = true;
                    updateUI();
                    loadCostHistory();
                } else {
                    errorLabel.setText("Failed to update treatment cost. Please try again.");
                }
            } catch (NumberFormatException nfe) {
                errorLabel.setText("Invalid cost entered.");
            } catch (Exception ex) {
                errorLabel.setText("Error updating cost: " + ex.getMessage());
            }
        }
    }
    
    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
        updateUI();
    }
    
    private void setupTableColumns() {
        // Payment table columns
        paymentDateCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getPaymentDate() != null ? dateFormatter.format(cd.getValue().getPaymentDate()) : ""
        ));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        
        // Format amount column
        amountCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(currencyFormat.format(amount));
                    setStyle(amount > 0 ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "");
                }
            }
        });
        
        // Cost history table columns
        effectiveDateCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getEffectiveFrom() != null ? dateFormatter.format(cd.getValue().getEffectiveFrom()) : ""
        ));
        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
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
        // Ensure there is a Close button even if FXML didn't define any
        if (!dialogPane.getButtonTypes().contains(ButtonType.CLOSE)) {
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        }
        Button closeButton = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setText("Close");
            closeButton.setOnAction(event -> dialogPane.getScene().getWindow().hide());
        }
    }
    
    private void updateUI() {
        if (treatment == null) return;
        
        // Update header
        titleLabel.setText("Treatment #" + treatment.getTreatmentId());
        subtitleLabel.setText("For: " + treatment.getPatientId()); // TODO: Replace with actual patient name
        
        // Update treatment details
        treatmentIdLabel.setText(treatment.getTreatmentId());
        boolean isActive = treatment.isActive();
        statusLabel.setText(isActive ? "Active" : "Completed");
        statusLabel.setStyle(isActive ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "-fx-text-fill: #757575;");
        if (markCompletedBtn != null) {
            // Always show the button; toggle text and action depending on state
            markCompletedBtn.setVisible(true);
            markCompletedBtn.setManaged(true);
            if (isActive) {
                markCompletedBtn.setText("Mark Completed");
                markCompletedBtn.setOnAction(e -> handleMarkCompleted());
            } else {
                markCompletedBtn.setText("Reopen Treatment");
                markCompletedBtn.setOnAction(e -> handleReopenTreatment());
            }
        }

        // Enable/disable editing actions based on status
        boolean editEnabled = isActive;
        if (addPaymentBtn != null) addPaymentBtn.setDisable(!editEnabled);
        if (deletePaymentBtn != null) deletePaymentBtn.setDisable(!editEnabled || paymentsTable.getSelectionModel().getSelectedItem() == null);
        if (updateCostBtn != null) updateCostBtn.setDisable(!editEnabled);
        
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
        if (treatment == null) return;
        try {
            List<Payment> payments = treatmentDao.getPaymentsForTreatment(treatment.getTreatmentId());
            paymentsTable.getItems().setAll(payments);
            if (deletePaymentBtn != null) {
                deletePaymentBtn.setDisable(paymentsTable.getSelectionModel().getSelectedItem() == null);
            }
        } catch (Exception ex) {
            errorLabel.setText("Failed to load payments: " + ex.getMessage());
        }
    }
    
    private void loadCostHistory() {
        if (treatment == null) return;
        try {
            List<TreatmentCost> costs = treatmentDao.getCostHistoryForTreatment(treatment.getTreatmentId());
            costHistoryTable.getItems().setAll(costs);
        } catch (Exception ex) {
            errorLabel.setText("Failed to load cost history: " + ex.getMessage());
        }
    }
    
    @FXML
    private void handleMarkCompleted() {
        if (treatment == null || !treatment.isActive()) return;

        // Confirm complete and close message (include pending info if any)
        String pendingMsg = treatment.getAmountPending() > 0.005
                ? String.format("\nPending: %s", currencyFormat.format(treatment.getAmountPending()))
                : "";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Complete Treatment");
        confirm.setHeaderText("Complete and close this treatment?");
        confirm.setContentText("You can reopen it later if needed." + pendingMsg);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        // Mark inactive (completed) and persist
        treatment.setActive(false);
        boolean ok = treatmentDao.updateTreatment(treatment);
        if (ok) {
            dataChanged = true;
            updateUI();
        } else {
            errorLabel.setText("Failed to mark completed. Please try again.");
        }
    }

    private void handleReopenTreatment() {
        if (treatment == null || treatment.isActive()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reopen Treatment");
        confirm.setHeaderText("Reopen this treatment?");
        confirm.setContentText("This will mark the treatment as Active and allow further updates.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        treatment.setActive(true);
        boolean ok = treatmentDao.updateTreatment(treatment);
        if (ok) {
            dataChanged = true;
            updateUI();
        } else {
            errorLabel.setText("Failed to reopen treatment. Please try again.");
        }
    }
    
    @FXML
    private void handleAddPayment() {
        if (treatment == null || !treatment.isActive()) {
            errorLabel.setText("Cannot add payment to a completed treatment.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null);
        dialog.setTitle("Add Payment");

        DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setTextFormatter(new TextFormatter<>(c -> {
            // allow numbers and dot
            if (c.getControlNewText().matches("^\\d*(\\.\\d{0,2})?$")) return c;
            return null;
        }));

        ChoiceBox<String> methodChoice = new ChoiceBox<>();
        methodChoice.getItems().addAll("CASH", "CARD", "UPI", "BANK_TRANSFER");
        methodChoice.getSelectionModel().select(treatment.getPaymentMethod() != null ? treatment.getPaymentMethod() : "CASH");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);

        grid.addRow(0, new Label("Amount:"), amountField);
        grid.addRow(1, new Label("Method:"), methodChoice);
        grid.addRow(2, new Label("Notes:"), notesArea);

        pane.setContent(grid);
        dialog.setDialogPane(pane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String amtText = amountField.getText();
                double amount = (amtText == null || amtText.isBlank()) ? 0.0 : Double.parseDouble(amtText);
                if (amount <= 0) {
                    errorLabel.setText("Payment amount must be greater than 0.");
                    return;
                }

                String method = methodChoice.getValue();
                String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

                // Confirm if overpaying beyond pending
                if (amount > treatment.getAmountPending()) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Overpayment");
                    confirm.setHeaderText("Payment exceeds pending amount");
                    confirm.setContentText(String.format("Pending: %s, Paying: %s. Continue?",
                            currencyFormat.format(treatment.getAmountPending()), currencyFormat.format(amount)));
                    Optional<ButtonType> c = confirm.showAndWait();
                    if (c.isEmpty() || c.get() != ButtonType.OK) {
                        return;
                    }
                }

                boolean ok = treatmentDao.recordPayment(treatment, amount, method, notes);
                if (ok) {
                    // Update local model and UI
                    treatment.recordPayment(amount);
                    treatment.setPaymentMethod(method);
                    if (!notes.isEmpty()) treatment.setNotes(notes);
                    dataChanged = true;
                    updateUI(); // refresh labels and totals
                    loadPaymentHistory();
                    loadCostHistory(); // refresh status in cost history
                } else {
                    errorLabel.setText("Failed to record payment. Please try again.");
                }
            } catch (NumberFormatException nfe) {
                errorLabel.setText("Invalid amount entered.");
            } catch (Exception ex) {
                errorLabel.setText("Error adding payment: " + ex.getMessage());
            }
        }
    }
    
    public boolean showAndWait(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Treatment Details");
        
        // Set the dialog content
        dialog.setDialogPane(dialogPane);
        // Ensure it's large enough and resizable
        dialog.setResizable(true);
        dialogPane.setPrefWidth(800);
        dialogPane.setPrefHeight(600);
        
        // Show the dialog and wait for response
        dialog.showAndWait();
        
        return dataChanged;
    }

    @FXML
    private void handleDeletePayment() {
        if (paymentsTable == null) return;
        Payment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (treatment == null || selected == null) return;
        if (!treatment.isActive()) {
            errorLabel.setText("Cannot delete payment from a completed treatment.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Payment");
        confirm.setHeaderText("Delete the selected payment?");
        confirm.setContentText(String.format("This will subtract %s from Amount Paid and update status.",
                currencyFormat.format(selected.getAmount())));
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        boolean ok = treatmentDao.deletePayment(selected);
        if (ok) {
            // Prefer reading fresh values from DB so generated columns are consistent
            try {
                java.util.Optional<Treatment> fresh = treatmentDao.findById(treatment.getId());
                if (fresh.isPresent()) {
                    Treatment t = fresh.get();
                    treatment.setTotalAmount(t.getTotalAmount());
                    treatment.setAmountPaid(t.getAmountPaid());
                    treatment.setAmountPending(t.getAmountPending());
                    treatment.setActive(t.isActive());
                    treatment.setUpdatedAt(t.getUpdatedAt());
                } else {
                    // Fallback: adjust locally
                    double amt = selected.getAmount();
                    double newPaid = Math.max(0, treatment.getAmountPaid() - amt);
                    treatment.setAmountPaid(newPaid);
                    treatment.setAmountPending(Math.max(0, treatment.getTotalAmount() - newPaid));
                    treatment.setUpdatedAt(java.time.LocalDateTime.now());
                }
            } catch (Exception ignored) {
                // If refresh fails, keep local fallback values
            }
            dataChanged = true;
            updateUI();
            loadPaymentHistory();
            loadCostHistory();
        } else {
            errorLabel.setText("Failed to delete payment. Please try again.");
        }
    }
}
