package com.nirwan.dentalclinic.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Payment {
    private int id;
    private String treatmentId;
    private double amount;
    private LocalDateTime paymentDate;
    private String paymentMethod; // CASH, CARD, UPI, BANK_TRANSFER
    private String transactionReference;
    private String notes;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
