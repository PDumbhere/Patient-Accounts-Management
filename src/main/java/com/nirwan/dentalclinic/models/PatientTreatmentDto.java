package com.nirwan.dentalclinic.models;

import javafx.beans.property.*;
import java.time.LocalDate;

public class PatientTreatmentDto {
    private final LongProperty patientId = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty treatmentId = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final DoubleProperty totalAmount = new SimpleDoubleProperty();
    private final DoubleProperty amountPaid = new SimpleDoubleProperty();
    private final DoubleProperty amountPending = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> treatmentDate = new SimpleObjectProperty<>();

    // Property getters for JavaFX binding
    public LongProperty patientIdProperty() { return patientId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty treatmentIdProperty() { return treatmentId; }
    public StringProperty descriptionProperty() { return description; }
    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public DoubleProperty amountPaidProperty() { return amountPaid; }
    public DoubleProperty amountPendingProperty() { return amountPending; }
    public StringProperty statusProperty() { return status; }
    public ObjectProperty<LocalDate> treatmentDateProperty() { return treatmentDate; }

    // Regular getters
    public Long getPatientId() { return patientId.get(); }
    public String getName() { return name.get(); }
    public String getTreatmentId() { return treatmentId.get(); }
    public String getDescription() { return description.get(); }
    public double getTotalAmount() { return totalAmount.get(); }
    public double getAmountPaid() { return amountPaid.get(); }
    public double getAmountPending() { return amountPending.get(); }
    public String getStatus() { return status.get(); }
    public LocalDate getTreatmentDate() { return treatmentDate.get(); }
}
