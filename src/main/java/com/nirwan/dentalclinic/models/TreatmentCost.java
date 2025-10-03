package com.nirwan.dentalclinic.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TreatmentCost {
    private int id;
    private String treatmentId;
    private double cost;
    private String status; // PENDING, PARTIALLY_PAID, PAID
    private String notes;
    private LocalDateTime effectiveFrom;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
