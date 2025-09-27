package com.nirwan.dentalclinic.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PatientListDto {
    private String patientName;
    private int age;
    private String recentTreatment;
    private String treatmentDate;
    private double balancePayment;
}
