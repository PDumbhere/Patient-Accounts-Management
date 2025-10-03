package com.nirwan.dentalclinic.repository;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.Patient;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientDao {
    private static final String INSERT_SQL = 
        "INSERT INTO patient (name, is_deleted, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?)";
    private static final String SELECT_ALL_SQL = "SELECT * FROM patient WHERE is_deleted = false";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM patient WHERE id = ? AND is_deleted = false";

    // CREATE a new patient
    public boolean savePatient(Patient patient) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, patient.getName());
                stmt.setBoolean(2, false); // is_deleted
                LocalDateTime now = LocalDateTime.now();
                stmt.setTimestamp(3, Timestamp.valueOf(now));
                stmt.setTimestamp(4, Timestamp.valueOf(now));
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            patient.setId(generatedKeys.getInt(1));
                            conn.commit();
                            return true;
                        }
                    }
                }
                conn.rollback();
                return false;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // FIND patient by ID
    public Optional<Patient> findById(Long id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Patient patient = new Patient();
                    patient.setId(rs.getInt("id"));
                    patient.setName(rs.getString("name"));
                    patient.setDeleted(rs.getBoolean("is_deleted"));
                    patient.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    patient.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return Optional.of(patient);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // READ all patients
    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Patient patient = new Patient();
                patient.setId(rs.getInt("id"));
                patient.setName(rs.getString("name"));
                patient.setDeleted(rs.getBoolean("is_deleted"));
                patient.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                patient.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                patients.add(patient);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    // UPDATE a patient
    public static void updatePatient(Patient patient) {
        String query = "UPDATE Patient SET name = ?, age = ?, mobile = ?, gender = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, patient.getName());
            pstmt.setInt(5, patient.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating patient: " + e.getMessage());
        }
    }

    // DELETE a patient
    public static void deletePatient(int id) {
        String query = "DELETE FROM Patient WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting patient: " + e.getMessage());
        }
    }
}

