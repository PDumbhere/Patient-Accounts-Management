package com.nirwan.dentalclinic.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;

public class MainController {
    @FXML private TableView<?> patientTable;
    @FXML private TableColumn<?, ?> colPatientName, colPatientAge, colRecentTreatment, colTreatmentDate, colBalancePayment;
    @FXML private Button btnAddPatient, btnUpdatePatient, btnExportData;

    @FXML
    public void initialize() {
        // Initialize table data (To be implemented)
    }
}
