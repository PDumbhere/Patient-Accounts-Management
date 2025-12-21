package com.nirwan.dentalclinic.models;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class PaymentReportRow {
    private final StringProperty patientName = new SimpleStringProperty();
    private final StringProperty treatmentDescription = new SimpleStringProperty();
    private final StringProperty treatmentId = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty paymentMethod = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> dateTime = new SimpleObjectProperty<>();

    public String getPatientName() { return patientName.get(); }
    public void setPatientName(String value) { patientName.set(value); }
    public StringProperty patientNameProperty() { return patientName; }

    public String getTreatmentDescription() { return treatmentDescription.get(); }
    public void setTreatmentDescription(String value) { treatmentDescription.set(value); }
    public StringProperty treatmentDescriptionProperty() { return treatmentDescription; }

    public String getTreatmentId() { return treatmentId.get(); }
    public void setTreatmentId(String value) { treatmentId.set(value); }
    public StringProperty treatmentIdProperty() { return treatmentId; }

    public double getAmount() { return amount.get(); }
    public void setAmount(double value) { amount.set(value); }
    public DoubleProperty amountProperty() { return amount; }

    public String getPaymentMethod() { return paymentMethod.get(); }
    public void setPaymentMethod(String value) { paymentMethod.set(value); }
    public StringProperty paymentMethodProperty() { return paymentMethod; }

    public LocalDateTime getDateTime() { return dateTime.get(); }
    public void setDateTime(LocalDateTime value) { dateTime.set(value); }
    public ObjectProperty<LocalDateTime> dateTimeProperty() { return dateTime; }
}
