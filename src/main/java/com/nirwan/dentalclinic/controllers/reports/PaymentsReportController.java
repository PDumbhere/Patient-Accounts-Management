package com.nirwan.dentalclinic.controllers.reports;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.PaymentReportRow;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentsReportController {
    @FXML private ComboBox<String> datePresetCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label fromLabel;
    @FXML private Label toLabel;
    @FXML private TextField treatmentFilterField;
    @FXML private ComboBox<String> paymentModeCombo;
    @FXML private TableView<PaymentReportRow> paymentsTable;
    @FXML private TableColumn<PaymentReportRow, String> colDate;
    @FXML private TableColumn<PaymentReportRow, String> colPatient;
    @FXML private TableColumn<PaymentReportRow, String> colTreatment;
    @FXML private TableColumn<PaymentReportRow, Number> colAmount;
    @FXML private TableColumn<PaymentReportRow, String> colMode;
    @FXML private Label totalEarningsLabel;
    @FXML private Label totalCashLabel;
    @FXML private Label totalOnlineLabel;

    private final ObservableList<PaymentReportRow> rows = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    public void initialize() {
        setupControls();
        setupTable();
        loadData();

        // Auto refresh when filters change
        treatmentFilterField.textProperty().addListener((o, a, b) -> loadData());
        paymentModeCombo.valueProperty().addListener((o, a, b) -> loadData());
        datePresetCombo.valueProperty().addListener((o, a, b) -> { toggleDatePickers(); loadData(); });
        if (fromDatePicker != null) fromDatePicker.valueProperty().addListener((o,a,b)-> loadData());
        if (toDatePicker != null) toDatePicker.valueProperty().addListener((o,a,b)-> loadData());
    }

    private void setupControls() {
        datePresetCombo.getItems().setAll(
                "Last 7 Days",
                "Last Month",
                "Last 3 Months",
                "Last 6 Months",
                "Last Year",
                "Custom Range"
        );
        datePresetCombo.getSelectionModel().select("Last 7 Days");

        // default custom range
        if (fromDatePicker != null && toDatePicker != null) {
            LocalDate today = LocalDate.now();
            fromDatePicker.setValue(today.withDayOfMonth(1));
            toDatePicker.setValue(today);
        }

        paymentModeCombo.getItems().setAll("All", "CASH", "CARD", "UPI", "BANK_TRANSFER");
        paymentModeCombo.getSelectionModel().select("All");
        
        // toggle date pickers visibility state
        toggleDatePickers();
    }

    private void setupTable() {
        // date as formatted string
        colDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getDateTime() != null ? dtf.format(cd.getValue().getDateTime()) : ""));
        // basic properties via PropertyValueFactory as per FXML
        paymentsTable.setItems(rows);
        // amount formatting
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText("");
                } else {
                    setText(String.format("₹%.2f", value.doubleValue()));
                    setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                }
            }
        });
    }

    private LocalDateTime[] computeRange() {
        String preset = datePresetCombo.getValue();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime start;
        LocalDateTime end;
        if ("Custom Range".equals(preset)) {
            LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : null;
            LocalDate to = toDatePicker != null ? toDatePicker.getValue() : null;
            if (from == null && to == null) {
                start = LocalDate.of(1970,1,1).atStartOfDay();
                end = LocalDateTime.now();
            } else {
                start = (from != null ? from : today).atStartOfDay();
                LocalDate toUse = (to != null ? to : today);
                end = toUse.plusDays(1).atStartOfDay().minusNanos(1);
            }
        } else {
            start = switch (preset) {
                case "Last 7 Days" -> today.minusDays(7).atStartOfDay();
                case "Last Month" -> today.minusMonths(1).atStartOfDay();
                case "Last 3 Months" -> today.minusMonths(3).atStartOfDay();
                case "Last 6 Months" -> today.minusMonths(6).atStartOfDay();
                case "Last Year" -> today.minusYears(1).atStartOfDay();
                default -> today.minusDays(7).atStartOfDay();
            };
            end = LocalDateTime.now();
        }
        return new LocalDateTime[]{start, end};
    }


    @FXML
    private void handleClose() {
        ((Control) paymentsTable).getScene().getWindow().hide();
    }

    private void loadData() {
        rows.clear();
        LocalDateTime[] range = computeRange();
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];
        String modeFilter = paymentModeCombo.getValue();
        String treatmentLike = treatmentFilterField.getText() != null ? treatmentFilterField.getText().trim() : "";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.name AS patient_name, t.description AS treatment_desc, ")
           .append("pay.amount, pay.payment_method, pay.payment_date, t.description AS treatment ")
           .append("FROM Payment pay ")
           .append("JOIN Treatment t ON pay.treatment_id = t.treatment_id ")
           .append("JOIN Patient p ON t.patient_id = p.id ")
           .append("WHERE pay.is_deleted = false AND pay.payment_date >= ? AND pay.payment_date <= ? ");
        if (modeFilter != null && !modeFilter.equals("All")) {
            sql.append("AND pay.payment_method = ? ");
        }
        if (!treatmentLike.isEmpty()) {
            sql.append("AND LOWER(t.description) LIKE ? ");
        }
        sql.append("ORDER BY pay.payment_date DESC");

        double total = 0.0, cash = 0.0, online = 0.0;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setTimestamp(idx++, Timestamp.valueOf(from));
            ps.setTimestamp(idx++, Timestamp.valueOf(to));
            if (modeFilter != null && !modeFilter.equals("All")) {
                ps.setString(idx++, modeFilter);
            }
            if (!treatmentLike.isEmpty()) {
                ps.setString(idx++, "%" + treatmentLike.toLowerCase() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentReportRow row = new PaymentReportRow();
                    row.setPatientName(rs.getString("patient_name"));
                    row.setTreatmentDescription(rs.getString("treatment_desc"));
                    row.setTreatmentId(rs.getString("treatment"));
                    row.setAmount(rs.getDouble("amount"));
                    row.setPaymentMethod(rs.getString("payment_method"));
                    Timestamp ts = rs.getTimestamp("payment_date");
                    row.setDateTime(ts != null ? ts.toLocalDateTime() : null);
                    rows.add(row);

                    total += row.getAmount();
                    if ("CASH".equalsIgnoreCase(row.getPaymentMethod())) cash += row.getAmount();
                    else online += row.getAmount();
                }
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Error loading payments: " + ex.getMessage()).showAndWait();
        }

        totalEarningsLabel.setText(String.format("₹%.2f", total));
        totalCashLabel.setText(String.format("₹%.2f", cash));
        totalOnlineLabel.setText(String.format("₹%.2f", online));
    }

    private void toggleDatePickers() {
        boolean custom = "Custom Range".equals(datePresetCombo.getValue());
        if (fromDatePicker != null) {
            fromDatePicker.setDisable(!custom);
            fromDatePicker.setVisible(custom);
            fromDatePicker.setManaged(custom);
        }
        if (toDatePicker != null) {
            toDatePicker.setDisable(!custom);
            toDatePicker.setVisible(custom);
            toDatePicker.setManaged(custom);
        }
        if (fromLabel != null) {
            fromLabel.setVisible(custom);
            fromLabel.setManaged(custom);
        }
        if (toLabel != null) {
            toLabel.setVisible(custom);
            toLabel.setManaged(custom);
        }
    }
}
