package com.nirwan.dentalclinic.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a dental treatment for a patient.
 * Each treatment can have multiple payments and cost updates.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Treatment {
    private int id;
    private String treatmentId;
    private int patientId;
    private String treatmentName;
    private double totalAmount;
    private double amountPaid;
    private double amountPending;
    private boolean isActive;
    private boolean isDeleted;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String paymentMethod;
    private String notes;

    /**
     * Creates a new active treatment with current timestamps
     */
    public Treatment(String treatmentId, int patientId, String treatmentName, double initialCost) {
        this.treatmentId = treatmentId;
        this.patientId = patientId;
        this.treatmentName = treatmentName;
        this.totalAmount = initialCost;
        this.amountPaid = 0.0;
        this.amountPending = initialCost;
        this.isActive = true;
        this.isDeleted = false;
        this.paymentMethod = "CASH"; // Default payment method
        this.notes = "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the treatment with a new payment
     * @param paymentAmount amount being paid
     */
    public void recordPayment(double paymentAmount) {
        this.amountPaid += paymentAmount;
        this.amountPending = Math.max(0, this.totalAmount - this.amountPaid);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the treatment cost
     * @param newCost new treatment cost
     */
    public void updateCost(double newCost) {
        this.totalAmount = newCost;
        this.amountPending = Math.max(0, this.totalAmount - this.amountPaid);
        this.updatedAt = LocalDateTime.now();
    }
}
