package com.nirwan.dentalclinic.repository;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.Treatment;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.Optional;

public class TreatmentDao {
    private static final String INSERT_TREATMENT_SQL = 
        "INSERT INTO Treatment (treatment_id, patient_id, description, total_amount, amount_paid, is_active, is_deleted, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_TREATMENT_BY_ID = 
        "SELECT * FROM Treatment WHERE id = ? AND is_deleted = false";
    
    private static final String SELECT_TREATMENTS_BY_PATIENT = 
        "SELECT * FROM Treatment WHERE patient_id = ? AND is_deleted = false ORDER BY is_active DESC, created_at DESC";
    
    private static final String UPDATE_TREATMENT_SQL = 
        "UPDATE Treatment SET description = ?, total_amount = ?, amount_paid = ?, amount_pending = ?, " +
        "is_active = ?, updated_at = ? WHERE id = ?";
    
    private static final String SOFT_DELETE_TREATMENT_SQL = 
        "UPDATE Treatment SET is_deleted = true, updated_at = ? WHERE id = ?";
    
    private static final String ADD_PAYMENT_SQL = 
        "UPDATE Treatment SET amount_paid = amount_paid + ?, amount_pending = GREATEST(0, total_amount - (amount_paid + ?)), " +
        "updated_at = ? WHERE id = ?";
    
    private static final String ADD_TREATMENT_COST_SQL = 
        "INSERT INTO TreatmentCost (treatment_id, cost, status, effective_from, notes) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String ADD_PAYMENT_RECORD_SQL = 
        "INSERT INTO Payment (treatment_id, amount, payment_date, payment_method, notes) " +
        "VALUES (?, ?, ?, ?, ?)";

    /**
     * Saves a new treatment to the database
     * @param treatment the treatment to save
     * @return the saved treatment with generated ID, or null if the operation failed
     */
    /**
     * Generates a unique treatment ID using timestamp and random number
     * @return A unique treatment ID in the format TRMT-yyyyMMdd-HHmmss-XXXX
     */
    private String generateTreatmentId() {
        // Format: TRMT-yyyyMMdd-HHmmss-XXXX
        // Where XXXX is a random 4-digit number for uniqueness
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return String.format("TRMT-%s-%04d", timestamp, random);
    }
    
    public Treatment saveTreatment(Treatment treatment) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Generate a new treatment ID if one isn't provided
            if (treatment.getTreatmentId() == null || treatment.getTreatmentId().trim().isEmpty()) {
                treatment.setTreatmentId(generateTreatmentId());
            }
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_TREATMENT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                // Set parameters for treatment insertion
                stmt.setString(1, treatment.getTreatmentId());
                stmt.setInt(2, treatment.getPatientId());
                stmt.setString(3, treatment.getDescription());
                stmt.setDouble(4, treatment.getTotalAmount());
                stmt.setDouble(5, treatment.getAmountPaid());
//                stmt.setDouble(6, treatment.getAmountPending());
                stmt.setBoolean(6, treatment.isActive());
                stmt.setBoolean(7, false); // is_deleted
                stmt.setTimestamp(8, Timestamp.valueOf(treatment.getCreatedAt()));
                stmt.setTimestamp(9, Timestamp.valueOf(treatment.getUpdatedAt()));
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int generatedId = generatedKeys.getInt(1);
                            treatment.setId(generatedId);
                            
                            // Now add the treatment cost record
                            try (PreparedStatement costStmt = conn.prepareStatement(ADD_TREATMENT_COST_SQL)) {
                                // Determine status based on payment
                                String status = "PENDING";
                                if (treatment.getAmountPaid() >= treatment.getTotalAmount()) {
                                    status = "PAID";
                                } else if (treatment.getAmountPaid() > 0) {
                                    status = "PARTIALLY_PAID";
                                }
                                
                                costStmt.setString(1, treatment.getTreatmentId());
                                costStmt.setDouble(2, treatment.getTotalAmount());
                                costStmt.setString(3, status);
                                costStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                                costStmt.setString(5, "Initial treatment cost");
                                
                                int costRows = costStmt.executeUpdate();
                                if (costRows == 0) {
                                    throw new SQLException("Failed to create treatment cost record");
                                }
                            }
                            
                            // Record the payment (including zero payments)
                            try (PreparedStatement paymentStmt = conn.prepareStatement(ADD_PAYMENT_RECORD_SQL)) {
                                paymentStmt.setString(1, treatment.getTreatmentId());
                                paymentStmt.setDouble(2, treatment.getAmountPaid());
                                paymentStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                                paymentStmt.setString(4, treatment.getPaymentMethod() != null ? 
                                    treatment.getPaymentMethod() : "CASH");
                                String paymentNote = "Initial payment";
                                if (treatment.getNotes() != null && !treatment.getNotes().isEmpty()) {
                                    paymentNote += ": " + treatment.getNotes();
                                }
                                if (treatment.getAmountPaid() == 0) {
                                    paymentNote = "No initial payment" + 
                                        (treatment.getNotes() != null && !treatment.getNotes().isEmpty() ? 
                                        ": " + treatment.getNotes() : "");
                                }
                                paymentStmt.setString(5, paymentNote);
                                
                                paymentStmt.executeUpdate();
                            }
                            
                            conn.commit();
                            return treatment;
                        }
                    }
                }
                conn.rollback();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error saving treatment: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Finds a treatment by its ID
     * @param id the treatment ID
     * @return an Optional containing the treatment if found
     */
    public Optional<Treatment> findById(int id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_TREATMENT_BY_ID)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTreatment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding treatment: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Finds all treatments for a specific patient
     * @param patientId the patient ID
     * @return a list of treatments for the patient
     */
    public List<Treatment> findByPatientId(int patientId) {
        List<Treatment> treatments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_TREATMENTS_BY_PATIENT)) {
            
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    treatments.add(mapResultSetToTreatment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding treatments by patient: " + e.getMessage());
        }
        return treatments;
    }

    /**
     * Updates an existing treatment
     * @param treatment the treatment to update
     * @return true if the update was successful
     */
    public boolean updateTreatment(Treatment treatment) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_TREATMENT_SQL)) {
            
            stmt.setString(1, treatment.getDescription());
            stmt.setDouble(2, treatment.getTotalAmount());
            stmt.setDouble(3, treatment.getAmountPaid());
            stmt.setDouble(4, treatment.getAmountPending());
            stmt.setBoolean(5, treatment.isActive());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(7, treatment.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating treatment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Soft deletes a treatment by setting is_deleted to true
     * @param id the treatment ID to delete
     * @return true if the operation was successful
     */
    public boolean softDelete(int id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SOFT_DELETE_TREATMENT_SQL)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, id);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error soft deleting treatment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Records a payment for a treatment
     * @param treatmentId the treatment ID
     * @param amount the payment amount
     * @param paymentMethod the payment method
     * @param notes optional payment notes
     * @return true if the payment was recorded successfully
     */
    public boolean recordPayment(int treatmentId, double amount, String paymentMethod, String notes) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement updateStmt = conn.prepareStatement(ADD_PAYMENT_SQL);
                 PreparedStatement paymentStmt = conn.prepareStatement(ADD_PAYMENT_RECORD_SQL)) {
                
                // Update treatment amounts
                updateStmt.setDouble(1, amount);
                updateStmt.setDouble(2, amount);
                updateStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(4, treatmentId);
                
                int updated = updateStmt.executeUpdate();
                
                if (updated > 0) {
                    // Record payment
                    paymentStmt.setInt(1, treatmentId);
                    paymentStmt.setDouble(2, amount);
                    paymentStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    paymentStmt.setString(4, paymentMethod);
                    paymentStmt.setString(5, notes);
                    
                    paymentStmt.executeUpdate();
                    conn.commit();
                    return true;
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error recording payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new treatment cost record
     * @param treatmentId the treatment ID
     * @param cost the new cost
     * @param notes optional notes about the cost change
     * @return true if the cost was added successfully
     */
    public boolean addTreatmentCost(int treatmentId, double cost, String notes) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(ADD_TREATMENT_COST_SQL)) {
                stmt.setInt(1, treatmentId);
                stmt.setDouble(2, cost);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(4, notes);
                
                int inserted = stmt.executeUpdate();
                
                if (inserted > 0) {
                    // Update the treatment's total amount
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE Treatment SET total_amount = ?, updated_at = ? WHERE id = ?")) {
                        
                        updateStmt.setDouble(1, cost);
                        updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setInt(3, treatmentId);
                        
                        int updated = updateStmt.executeUpdate();
                        if (updated > 0) {
                            conn.commit();
                            return true;
                        }
                    }
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error adding treatment cost: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to map a ResultSet to a Treatment object
     */
    private Treatment mapResultSetToTreatment(ResultSet rs) throws SQLException {
        Treatment treatment = new Treatment();
        treatment.setId(rs.getInt("id"));
        treatment.setTreatmentId(rs.getString("treatment_id"));
        treatment.setPatientId(rs.getInt("patient_id"));
        treatment.setDescription(rs.getString("description"));
        treatment.setTotalAmount(rs.getDouble("total_amount"));
        treatment.setAmountPaid(rs.getDouble("amount_paid"));
        treatment.setAmountPending(rs.getDouble("amount_pending"));
        treatment.setActive(rs.getBoolean("is_active"));
        treatment.setDeleted(rs.getBoolean("is_deleted"));
        treatment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        treatment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return treatment;
    }
}
