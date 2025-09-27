package com.nirwan.dentalclinic.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PatientTreatmentData {
    Patient patient;
    Treatment treatment;
    List<Appointment> appointments;
    String status;
}