package com.nirwan.dentalclinic.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import java.time.LocalDate;

public class PatientRecord {
    private final SimpleStringProperty patientName;
    private final SimpleIntegerProperty age;
    private final SimpleStringProperty latestTreatment;
    private final LocalDate latestTreatmentDate;
    private final SimpleDoubleProperty balancePayment;

    public PatientRecord(String patientName, int age, String latestTreatment, 
                        LocalDate latestTreatmentDate, double balancePayment) {
        this.patientName = new SimpleStringProperty(patientName);
        this.age = new SimpleIntegerProperty(age);
        this.latestTreatment = new SimpleStringProperty(latestTreatment);
        this.latestTreatmentDate = latestTreatmentDate;
        this.balancePayment = new SimpleDoubleProperty(balancePayment);
    }

    // Getters for properties (needed for TableView)
    public String getPatientName() { return patientName.get(); }
    public int getAge() { return age.get(); }
    public String getLatestTreatment() { return latestTreatment.get(); }
    public LocalDate getLatestTreatmentDate() { return latestTreatmentDate; }
    public double getBalancePayment() { return balancePayment.get(); }

    // Property getters (needed for TableView binding)
    public SimpleStringProperty patientNameProperty() { return patientName; }
    public SimpleIntegerProperty ageProperty() { return age; }
    public SimpleStringProperty latestTreatmentProperty() { return latestTreatment; }
    public SimpleDoubleProperty balancePaymentProperty() { return balancePayment; }
}
