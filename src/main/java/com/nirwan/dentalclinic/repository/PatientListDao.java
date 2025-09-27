package com.nirwan.dentalclinic.repository;


import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.PatientListDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PatientListDao {

    private static final String GET_ALL_PATIENTS_LIST = "SELECT * FROM PatientList";

    public static List<PatientListDto> getAllPatientsList() {
        List<PatientListDto> patientList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_PATIENTS_LIST);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                patientList.add(new PatientListDto(
                        rs.getString("patientName"),
                        rs.getInt("age"),
                        rs.getString("latestTreatment"),
                        rs.getString("latestTreatmentDate"),
                        rs.getDouble("balancePayment")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching patient list: " + e.getMessage());
        }
        return patientList;
    }
}
