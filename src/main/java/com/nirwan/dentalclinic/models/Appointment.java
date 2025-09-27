package com.nirwan.dentalclinic.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Appointment {
    private int id;
    private int patientTreatmentMappingId;
    private String appointmentDate;
    private double paymentMade;
    private String remarks;

}
