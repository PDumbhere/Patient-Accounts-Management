package com.nirwan.dentalclinic.controllers.reports;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.*;
import com.nirwan.dentalclinic.repository.ExpensesDao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.prefs.Preferences;

@NoArgsConstructor
public class ExpensesReportController {
    public ComboBox<String> datePresetCombo;
    public Label fromLabel;
    public DatePicker fromDatePicker;
    public Label toLabel;
    public DatePicker toDatePicker;
    public ComboBox<String> expenseTypeCombo;
    public Button addExpense;
    public Button editExpense;
    public Button deleteExpense;
    public TableView<ExpenseReportRow> expensesTable;
    public TableColumn<ExpenseReportRow, String> paymentDate;
    public TableColumn<ExpenseReportRow, String> expenseType;
    public TableColumn<ExpenseReportRow, String> product;
    public TableColumn<ExpenseReportRow, String> nameTo;
    public TableColumn<ExpenseReportRow, Number> amount;
    public Label totalExpenditureLabel;
    public Label errorLabel;

    @FXML
    private BorderPane borderPane;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ExpensesDao expensesDao = new ExpensesDao();
    private final Preferences prefs = Preferences.userNodeForPackage(ExpensesReportController.class);
    private static final String PREF_LAST_EXPORT_DIR = "expenses_report_last_dir";
    private final ObservableList<ExpenseReportRow> expenseReportRows = FXCollections.observableArrayList();
    private ExpenseReportRow selectedExpense;

    @FXML
    public void initialize(){
        setupControls();
        setupTable();
        loadData();
        if (expenseTypeCombo != null) {
            expensesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (deleteExpense != null) {
                    deleteExpense.setDisable(newSel == null);
                }
                if (editExpense != null) {
                    editExpense.setDisable(newSel == null);
                }
                selectedExpense = newSel;
            });
        }
        expenseTypeCombo.valueProperty().addListener((o, a, b) -> loadData());
        datePresetCombo.valueProperty().addListener((o, a, b) -> { toggleDatePickers(); loadData(); });
        if (fromDatePicker != null) fromDatePicker.valueProperty().addListener((o,a,b)-> loadData());
        if (toDatePicker != null) toDatePicker.valueProperty().addListener((o,a,b)-> loadData());
    }

    private void setupTable() {
        // date as formatted string
        paymentDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getPaymentDate() != null ? dtf.format(cd.getValue().getPaymentDate()) : ""));
        // basic properties via PropertyValueFactory as per FXML
        expensesTable.setItems(expenseReportRows);
        // amount formatting
        amount.setCellFactory(tc -> new TableCell<>() {
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

        expenseTypeCombo.getItems().setAll(
                "All",
                "Material",
                "Lab",
                "Surgery",
                "Electric Bill",
                "Instrument",
                "Other"
        );
        expenseTypeCombo.setPromptText("Select Expense Type");

        // default custom range
        if (fromDatePicker != null && toDatePicker != null) {
            LocalDate today = LocalDate.now();
            fromDatePicker.setValue(today.withDayOfMonth(1));
            toDatePicker.setValue(today);
        }

        // toggle date pickers visibility state
        toggleDatePickers();
    }

    private void loadData() {
        expenseReportRows.clear();
        LocalDate[] range = computeRange();
        LocalDate from = range[0];
        LocalDate to = range[1];
        String type = expenseTypeCombo.getValue() != null ?  expenseTypeCombo.getValue() : "";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.id, e.transaction_date, e.type, e.product, e.name, e.amount ")
                .append("FROM Expenses e ")
                .append("WHERE e.is_deleted = false AND e.transaction_date >= ? AND e.transaction_date <= ? ");
        if (!type.isEmpty() && !"All".equalsIgnoreCase(type)) {
            sql.append("AND LOWER(e.type) LIKE ? ");
        }
        sql.append("ORDER BY e.transaction_date DESC");

        double total = 0.0;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(from));
            ps.setDate(idx++, Date.valueOf(to));
            if (!type.isEmpty() && !"All".equalsIgnoreCase(type)) {
                ps.setString(idx++, "%" + type.toLowerCase() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExpenseReportRow row = new ExpenseReportRow();
                    row.setId(rs.getInt("id"));
                    row.setPaymentDate(rs.getDate("transaction_date").toLocalDate());
                    row.setExpenseType(rs.getString("type"));
                    row.setProduct(rs.getString("product"));
                    row.setNameTo(rs.getString("name"));
                    row.setAmount(rs.getDouble("amount"));
                    expenseReportRows.add(row);

                    total += row.getAmount();
                }
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Error loading payments: " + ex.getMessage()).showAndWait();
        }
        expensesTable.setItems(expenseReportRows);
        totalExpenditureLabel.setText(String.format("₹%.2f", total));
    }

    private LocalDate[] computeRange() {
        String preset = datePresetCombo.getValue();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate start;
        LocalDate end;
        if ("Custom Range".equals(preset)) {
            LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : null;
            LocalDate to = toDatePicker != null ? toDatePicker.getValue() : null;
            if (from == null && to == null) {
                start = LocalDate.of(today.getYear(),1,1);
                end = LocalDate.now();
            } else {
                start = (from != null ? from : today);
                LocalDate toUse = (to != null ? to : today);
                end = toUse.plusDays(1);
            }
        } else {
            start = switch (preset) {
                case "Last 7 Days" -> today.minusDays(7);
                case "Last Month" -> today.minusMonths(1);
                case "Last 3 Months" -> today.minusMonths(3);
                case "Last 6 Months" -> today.minusMonths(6);
                case "Last Year" -> today.minusYears(1);
                default -> today.minusDays(7);
            };
            end = LocalDate.now();
        }
        return new LocalDate[]{start, end};
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

    @FXML
    private void handleAddPayment() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(borderPane.getScene() != null ? borderPane.getScene().getWindow() : null);
        dialog.setTitle("Add Expense");
        Stage dialogStage = (Stage) dialog.getDialogPane()
                .getScene().getWindow();
        dialogStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icons/icon.png")));

        DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ComboBox typeComboBox = new ComboBox();
        typeComboBox.getItems().setAll(
                "Material",
                "Lab",
                "Surgery",
                "Electric Bill",
                "Instrument",
                "Other"
        );
        typeComboBox.setPromptText("Select Expense Type");
        TextField productField = new TextField();
        productField.setPromptText("Enter Product name/description");
        TextField nameTo = new TextField();
        nameTo.setPromptText("Enter Receiver Name");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setTextFormatter(new TextFormatter<>(c -> {
            // allow numbers and dot
            if (c.getControlNewText().matches("^\\d*(\\.\\d{0,2})?$")) return c;
            return null;
        }));

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.setPromptText("Date");

        

        grid.addRow(0, new Label("Expense Type:"), typeComboBox);
        grid.addRow(1,new Label("Product:"), productField);
        grid.addRow(2, new Label("Name To:"), nameTo);
        grid.addRow(3, new Label("Amount"), amountField);
        grid.addRow(4, new Label("Payment Date"), datePicker);

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

                String type = (String) typeComboBox.getValue();
                String product = productField.getText() != null ? productField.getText().trim() : "";
                String name = nameTo.getText()!=null?nameTo.getText().trim() :"";
                LocalDate paymentDate = datePicker.getValue();

                Expenses expense = new Expenses();
                expense.setName(name);
                expense.setType(type);
                expense.setProduct(product);
                expense.setAmount(amount);
                expense.setPaymentDate(paymentDate);
                boolean ok = expensesDao.saveExpense(expense);
                if (ok) {
                    // Update local model and UI
//                    dataChanged = true;
//                    updateUI(); // refresh labels and totals
//                    loadPaymentHistory();
                    loadData(); // refresh status in cost history
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

    @FXML
    private void handleEditPayment() {
        if (selectedExpense != null) {
            showEditExpenseDialog(selectedExpense);
        }
    }

    private void showEditExpenseDialog(ExpenseReportRow expenseRow) {
        if (expenseRow == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(deleteExpense.getScene() != null ? borderPane.getScene().getWindow() : null);
        dialog.setTitle("Add Expense");
        Stage dialogStage = (Stage) dialog.getDialogPane()
                .getScene().getWindow();
        dialogStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icons/icon.png")));

        DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ComboBox typeComboBox = new ComboBox();
        typeComboBox.getItems().setAll(
                "Material",
                "Lab",
                "Surgery",
                "Electric Bill",
                "Instrument",
                "Other"
        );
        typeComboBox.setValue(expenseRow.getExpenseType());
        typeComboBox.setPromptText("Select Expense Type");
        TextField productField = new TextField();
        productField.setText(expenseRow.getProduct());
        productField.setPromptText("Enter Product name/description");
        TextField nameTo = new TextField();
        nameTo.setText(expenseRow.getNameTo());
        nameTo.setPromptText("Enter Receiver Name");
        TextField amountField = new TextField();
        amountField.setText(String.valueOf(expenseRow.getAmount()));
        amountField.setPromptText("Amount");
        amountField.setTextFormatter(new TextFormatter<>(c -> {
            // allow numbers and dot
            if (c.getControlNewText().matches("^\\d*(\\.\\d{0,2})?$")) return c;
            return null;
        }));

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(expenseRow.getPaymentDate());
        datePicker.setPromptText("Date");



        grid.addRow(0, new Label("Expense Type:"), typeComboBox);
        grid.addRow(1,new Label("Product:"), productField);
        grid.addRow(2, new Label("Name To:"), nameTo);
        grid.addRow(3, new Label("Amount"), amountField);
        grid.addRow(4, new Label("Payment Date"), datePicker);

        pane.setContent(grid);
        dialog.setDialogPane(pane);

        // Validate input
        Node addButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        addButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Double.parseDouble(amountField.getText());
            } catch (NumberFormatException e) {
                event.consume();
                showAlert("Invalid Amount", "Please enter a valid amount", Alert.AlertType.ERROR);
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
               Expenses expenses = new Expenses();
               expenses.setId(expenseRow.getId());
               expenses.setType((String) typeComboBox.getValue());
               expenses.setProduct(productField.getText());
               expenses.setName(nameTo.getText());
               expenses.setAmount(Double.parseDouble(amountField.getText()));
               expenses.setPaymentDate(datePicker.getValue());
               Boolean ok = expensesDao.editExpense(expenses);
                if (ok) {
                    // Update local model and UI
//                    dataChanged = true;
//                    updateUI(); // refresh labels and totals
//                    loadPaymentHistory();
                    loadData(); // refresh status in cost history
                } else {
                    errorLabel.setText("Failed to record payment. Please try again.");
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid amount format", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleDeletePayment() {
        if (expensesTable == null) return;
        ExpenseReportRow selected = expensesTable.getSelectionModel().getSelectedItem();
        if (expenseReportRows == null || selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Expense");
        confirm.setHeaderText("Delete the selected Expenditure?");
        confirm.setContentText(String.format("This will permanently delete this expense record maid to %s" +
                        " for %s",
                selected.getNameTo(), selected.getProduct()));
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        boolean ok = expensesDao.deleteExpense(selected.getId());
        if (ok) {

//            dataChanged = true;
//            updateUI();
//            loadPaymentHistory();
//            loadCostHistory();
            loadData();
        } else {
            errorLabel.setText("Failed to delete payment. Please try again.");
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleExport() {
        if (expensesTable == null || expensesTable.getItems() == null || expensesTable.getItems().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Nothing to export. Adjust filters to show some rows.").showAndWait();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Expenses Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("expenses-report"
                +LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                +".xlsx");
        // Use last used directory if available
        try {
            String last = prefs.get(PREF_LAST_EXPORT_DIR, null);
            if (last != null) {
                File dir = new File(last);
                if (dir.isDirectory()) fc.setInitialDirectory(dir);
            }
        } catch (Exception ignored) {}
        File file = fc.showSaveDialog(expensesTable.getScene().getWindow());
        if (file == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Expenses");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor((short) 22); // light grey
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Amount style
            CellStyle amountStyle = wb.createCellStyle();
            amountStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("#,##0.00"));

            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] headers = {"id","Transaction Date", "Expense Type", "Product", "To Name", "Amount"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (ExpenseReportRow r : expensesTable.getItems()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId()!=null ? r.getId() : -1);
                row.createCell(1).setCellValue(r.getPaymentDate() != null ? dtf.format(r.getPaymentDate()) : "");
                row.createCell(2).setCellValue(r.getExpenseType() != null ? r.getExpenseType() : "");
                row.createCell(3).setCellValue(r.getProduct() != null ? r.getProduct() : "");
                row.createCell(4).setCellValue(r.getNameTo() != null ? r.getNameTo() : "");
                org.apache.poi.ss.usermodel.Cell amountCell = row.createCell(5);
                amountCell.setCellValue(r.getAmount());
                amountCell.setCellStyle(amountStyle);
            }

            // Total row (sum of Amount column)
            Row totalRow = sheet.createRow(rowIdx+2);
            // Bold style for total label
            CellStyle totalLabelStyle = wb.createCellStyle();
            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setFillForegroundColor((short) 22);
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Cell totalLabelCell = totalRow.createCell(2); // column C: "Treatment" column position for label
            totalLabelCell.setCellValue("Total");
            totalLabelCell.setCellStyle(totalLabelStyle);

            // Amount total with bold amount style
            CellStyle totalAmountStyle = wb.createCellStyle();
            totalAmountStyle.cloneStyleFrom(amountStyle);
            Font totalAmountFont = wb.createFont();
            totalAmountFont.setBold(true);
            totalAmountStyle.setFont(totalAmountFont);
            Cell totalAmountCell = totalRow.createCell(3); // column D: Amount
            int dataEndRow = rowIdx; // last data row index (1-based in Excel is +1)
            // In Excel rows are 1-based; header is row 1, data starts at row 2
            String formula = String.format("SUM(F2:F%d)", dataEndRow);
            totalAmountCell.setCellFormula(formula);
            totalAmountCell.setCellStyle(totalAmountStyle);

            // Autosize
            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
            // Remember the chosen directory
            try {
                File parent = file.getParentFile();
                if (parent != null) prefs.put(PREF_LAST_EXPORT_DIR, parent.getAbsolutePath());
            } catch (Exception ignored) {}
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to export: " + ex.getMessage()).showAndWait();
        }
    }
}
