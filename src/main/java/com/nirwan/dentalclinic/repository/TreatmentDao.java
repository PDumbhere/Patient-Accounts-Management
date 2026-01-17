package com.nirwan.dentalclinic.repository;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.Payment;
import com.nirwan.dentalclinic.models.Treatment;
import com.nirwan.dentalclinic.models.TreatmentCost;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.Optional;

public class TreatmentDao {
    private static final String INSERT_TREATMENT_SQL = 
        "INSERT INTO Treatment (treatment_id, patient_id, treatment_name, total_amount, amount_paid, is_active, is_deleted, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_TREATMENT_BY_ID = 
        "SELECT * FROM Treatment WHERE id = ? AND is_deleted = false";
    
    private static final String SELECT_TREATMENTS_BY_PATIENT = 
        "SELECT * FROM Treatment WHERE patient_id = ? AND is_deleted = false ORDER BY is_active DESC, created_at DESC";
    
    // Note: amount_pending is a generated column in DB, do not set it explicitly
    private static final String UPDATE_TREATMENT_SQL = 
        "UPDATE Treatment SET treatment_name = ?, total_amount = ?, amount_paid = ?, " +
        "is_active = ?, updated_at = ? WHERE id = ?";
    
    private static final String SOFT_DELETE_TREATMENT_SQL = 
        "UPDATE Treatment SET is_deleted = true, updated_at = ? WHERE id = ?";
    
    // Do not update amount_pending (generated). Let DB compute it.
    private static final String ADD_PAYMENT_SQL = 
        "UPDATE Treatment SET amount_paid = amount_paid + ?, updated_at = ? WHERE id = ?";
    
    private static final String ADD_TREATMENT_COST_SQL = 
        "INSERT INTO TreatmentCost (treatment_id, cost, status, effective_from, notes) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String ADD_PAYMENT_RECORD_SQL = 
        "INSERT INTO Payment (treatment_id, amount, payment_date, payment_method, notes) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String SOFT_DELETE_PAYMENT_SQL =
        "UPDATE Payment SET is_deleted = true, updated_at = ? WHERE id = ?";

    private static final String SELECT_PAYMENTS_BY_TREATMENT =
        "SELECT * FROM Payment WHERE treatment_id = ? AND is_deleted = false ORDER BY payment_date DESC";

    private static final String SELECT_COSTS_BY_TREATMENT =
        "SELECT * FROM TreatmentCost WHERE treatment_id = ? AND is_deleted = false ORDER BY effective_from DESC";

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
                stmt.setString(3, treatment.getTreatmentName());
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
                                    treatment.setAmountPending(0.0);
                                    status = "PAID";
                                } else if (treatment.getAmountPaid() > 0) {
                                    treatment.setAmountPending(treatment.getTotalAmount() - treatment.getAmountPaid());
                                    status = "PARTIALLY_PAID";
                                }
                                
                                costStmt.setString(1, treatment.getTreatmentId());
                                costStmt.setDouble(2, treatment.getTotalAmount());
                                costStmt.setString(3, status);
                                costStmt.setTimestamp(4, Timestamp.valueOf(treatment.getPaymentDate()));
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
                                paymentStmt.setTimestamp(3, Timestamp.valueOf(treatment.getPaymentDate()));
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
     * Updates the status of the latest TreatmentCost row for the given treatment code
     * to PAID / PARTIALLY_PAID / PENDING based on Treatment totals.
     */
    private void updateLatestTreatmentCostStatus(Connection conn, String treatmentCode) throws SQLException {
        String sql = "UPDATE TreatmentCost tc " +
                "JOIN Treatment t ON t.treatment_id = tc.treatment_id " +
                "JOIN (SELECT treatment_id, MAX(effective_from) AS max_eff FROM TreatmentCost WHERE treatment_id = ? AND is_deleted = false) latest " +
                "  ON tc.treatment_id = latest.treatment_id AND tc.effective_from = latest.max_eff " +
                // Consider small rounding differences when comparing currency amounts
                "SET tc.status = CASE WHEN ABS(t.total_amount - t.amount_paid) <= 0.005 OR t.amount_paid > t.total_amount THEN 'PAID' " +
                "                      WHEN t.amount_paid > 0 THEN 'PARTIALLY_PAID' " +
                "                      ELSE 'PENDING' END " +
                "WHERE tc.treatment_id = ? AND tc.is_deleted = false";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, treatmentCode);
            ps.setString(2, treatmentCode);
            ps.executeUpdate();
        }
    }

    /**
     * Records a payment for a treatment, using both the numeric primary key (for Treatment update)
     * and the string code (for Payment.treatment_id foreign key)
     */
    public boolean recordPayment(Treatment treatment, double amount,
                                 String paymentMethod, String notes,
                                 LocalDateTime paymentDate) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = conn.prepareStatement(ADD_PAYMENT_SQL);
                 PreparedStatement paymentStmt = conn.prepareStatement(ADD_PAYMENT_RECORD_SQL)) {

                // Update treatment (by numeric id)
                updateStmt.setDouble(1, amount);
                updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(3, treatment.getId());

                int updated = updateStmt.executeUpdate();

                if (updated > 0) {
                    // Record payment (by string treatment code)
                    paymentStmt.setString(1, treatment.getTreatmentId());
                    paymentStmt.setDouble(2, amount);
                    paymentStmt.setTimestamp(3, Timestamp.valueOf(paymentDate));
                    paymentStmt.setString(4, paymentMethod);
                    paymentStmt.setString(5, notes);

                    paymentStmt.executeUpdate();

                    // Update latest TreatmentCost status based on current Treatment totals
                    updateLatestTreatmentCostStatus(conn, treatment.getTreatmentId());
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
     * Returns all payment records for a given treatment code (treatment_id)
     */
    public List<Payment> getPaymentsForTreatment(String treatmentId) {
        List<Payment> payments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PAYMENTS_BY_TREATMENT)) {
            stmt.setString(1, treatmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching payments: " + e.getMessage());
        }
        return payments;
    }

    /**
     * Returns treatment cost history for a given treatment code (treatment_id)
     */
    public List<TreatmentCost> getCostHistoryForTreatment(String treatmentId) {
        List<TreatmentCost> costs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_COSTS_BY_TREATMENT)) {
            stmt.setString(1, treatmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    costs.add(mapResultSetToTreatmentCost(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching treatment costs: " + e.getMessage());
        }
        return costs;
    }

    /**
     * Edits an existing payment record and updates the associated treatment's paid amount.
     * Uses the string treatment code for FK consistency.
     */
    public boolean editPayment(Payment oldPayment, Payment newPayment) {
        if (oldPayment == null || newPayment == null) return false;

        String treatmentCode = oldPayment.getTreatmentId();
        if (treatmentCode == null || !treatmentCode.equals(newPayment.getTreatmentId())) {
            System.err.println("Payment treatment codes don't match or are missing; cannot edit payment.");
            return false;
        }

        double amountDiff = newPayment.getAmount() - oldPayment.getAmount();
        if (amountDiff == 0 &&
                (oldPayment.getPaymentMethod() == null ? newPayment.getPaymentMethod() == null :
                        oldPayment.getPaymentMethod().equals(newPayment.getPaymentMethod())) &&
                (oldPayment.getNotes() == null ? newPayment.getNotes() == null :
                        oldPayment.getNotes().equals(newPayment.getNotes()))) {
            // No changes to make
            return true;
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Update payment record
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE Payment SET amount = ?, payment_date = ?, payment_method = ?, notes = ?, updated_at = ? " +
                                "WHERE id = ?")) {

                    updateStmt.setDouble(1, newPayment.getAmount());
                    updateStmt.setTimestamp(2, Timestamp.valueOf(newPayment.getPaymentDate()));
                    updateStmt.setString(3, newPayment.getPaymentMethod());
                    updateStmt.setString(4, newPayment.getNotes());
                    updateStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    updateStmt.setInt(6, oldPayment.getId());

                    int updated = updateStmt.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // Update treatment's paid amount if the amount changed
                if (amountDiff != 0) {
                    try (PreparedStatement updateTreatment = conn.prepareStatement(
                            "UPDATE Treatment SET amount_paid = GREATEST(0, amount_paid + ?), updated_at = ? " +
                                    "WHERE treatment_id = ?")) {

                        updateTreatment.setDouble(1, amountDiff);
                        updateTreatment.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        updateTreatment.setString(3, treatmentCode);

                        int updated = updateTreatment.executeUpdate();
                        if (updated == 0) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // Refresh latest cost status
                updateLatestTreatmentCostStatus(conn, treatmentCode);
                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error updating payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Soft deletes the given payment and decrements Treatment.amount_paid accordingly.
     * Uses the string treatment code for FK consistency.
     */
    public boolean deletePayment(Payment payment) {
        if (payment == null) return false;
        String treatmentCode = payment.getTreatmentId();
        double amount = payment.getAmount();
        if (treatmentCode == null) {
            // Fallback: try to resolve code from DB via payment id
            // For simplicity, require treatmentCode on the Payment object
            System.err.println("Payment missing treatmentCode; cannot delete reliably.");
            return false;
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement del = conn.prepareStatement(SOFT_DELETE_PAYMENT_SQL);
                 PreparedStatement dec = conn.prepareStatement(
                         "UPDATE Treatment SET amount_paid = GREATEST(0, amount_paid - ?), updated_at = ? WHERE treatment_id = ?")) {

                // Soft delete payment
                del.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                del.setInt(2, payment.getId());
                int d = del.executeUpdate();

                if (d > 0) {
                    // Decrement treatment.amount_paid by this payment amount
                    dec.setDouble(1, amount);
                    dec.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    dec.setString(3, treatmentCode);
                    int u = dec.executeUpdate();
                    if (u > 0) {
                        // Refresh latest cost status
                        updateLatestTreatmentCostStatus(conn, treatmentCode);
                        conn.commit();
                        return true;
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
            System.err.println("Error deleting payment: " + e.getMessage());
            return false;
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
            
            stmt.setString(1, treatment.getTreatmentName());
            stmt.setDouble(2, treatment.getTotalAmount());
            stmt.setDouble(3, treatment.getAmountPaid());
            stmt.setBoolean(4, treatment.isActive());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(6, treatment.getId());
            
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
                updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(3, treatmentId);
                
                int updated = updateStmt.executeUpdate();
                
                if (updated > 0) {
                    // Lookup string treatment code for Payment FK
                    String treatmentCode = findById(treatmentId).map(Treatment::getTreatmentId).orElse(null);
                    if (treatmentCode == null) {
                        conn.rollback();
                        return false;
                    }
                    // Record payment with string treatment_id
                    paymentStmt.setString(1, treatmentCode);
                    paymentStmt.setDouble(2, amount);
                    paymentStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    paymentStmt.setString(4, paymentMethod);
                    paymentStmt.setString(5, notes);
                    
                    paymentStmt.executeUpdate();

                    // Update latest TreatmentCost status based on current Treatment totals
                    updateLatestTreatmentCostStatus(conn, treatmentCode);
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
     * Adds a new treatment cost record and updates Treatment.total_amount.
     * Status is computed based on amount_paid vs new cost.
     */
    public boolean addTreatmentCost(Treatment treatment, double cost, String notes) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(ADD_TREATMENT_COST_SQL)) {
                String status = (Math.abs(cost - treatment.getAmountPaid()) <= 0.005 || treatment.getAmountPaid() > cost)
                        ? "PAID"
                        : (treatment.getAmountPaid() > 0 ? "PARTIALLY_PAID" : "PENDING");

                stmt.setString(1, treatment.getTreatmentId());
                stmt.setDouble(2, cost);
                stmt.setString(3, status);
                stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(5, notes);

                int inserted = stmt.executeUpdate();

                if (inserted > 0) {
                    // Update treatment total_amount
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE Treatment SET total_amount = ?, updated_at = ? WHERE id = ?")) {

                        updateStmt.setDouble(1, cost);
                        updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setInt(3, treatment.getId());

                        int updated = updateStmt.executeUpdate();
                        if (updated > 0) {
                            // Ensure the latest cost row has correct status after DB update side effects
                            updateLatestTreatmentCostStatus(conn, treatment.getTreatmentId());
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
        treatment.setTreatmentName(rs.getString("treatment_name"));
        treatment.setTotalAmount(rs.getDouble("total_amount"));
        treatment.setAmountPaid(rs.getDouble("amount_paid"));
        treatment.setAmountPending(rs.getDouble("amount_pending"));
        treatment.setActive(rs.getBoolean("is_active"));
        treatment.setDeleted(rs.getBoolean("is_deleted"));
        treatment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        treatment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return treatment;
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        // Some code paths use string treatment_id, align here
        try {
            p.setTreatmentId(rs.getString("treatment_id"));
        } catch (SQLException ignored) { }
        p.setAmount(rs.getDouble("amount"));
        Timestamp ts = rs.getTimestamp("payment_date");
        if (ts != null) p.setPaymentDate(ts.toLocalDateTime());
        p.setPaymentMethod(rs.getString("payment_method"));
        // transaction_reference might not exist; guard it
        try { p.setTransactionReference(rs.getString("transaction_reference")); } catch (SQLException ignored) {}
        p.setNotes(rs.getString("notes"));
        try { p.setDeleted(rs.getBoolean("is_deleted")); } catch (SQLException ignored) {}
        try {
            Timestamp c = rs.getTimestamp("created_at");
            if (c != null) p.setCreatedAt(c.toLocalDateTime());
            Timestamp u = rs.getTimestamp("updated_at");
            if (u != null) p.setUpdatedAt(u.toLocalDateTime());
        } catch (SQLException ignored) {}
        return p;
    }

    private TreatmentCost mapResultSetToTreatmentCost(ResultSet rs) throws SQLException {
        TreatmentCost tc = new TreatmentCost();
        tc.setId(rs.getInt("id"));
        try { tc.setTreatmentId(rs.getString("treatment_id")); } catch (SQLException ignored) {}
        tc.setCost(rs.getDouble("cost"));
        tc.setStatus(rs.getString("status"));
        tc.setNotes(rs.getString("notes"));
        Timestamp eff = rs.getTimestamp("effective_from");
        if (eff != null) tc.setEffectiveFrom(eff.toLocalDateTime());
        try { tc.setDeleted(rs.getBoolean("is_deleted")); } catch (SQLException ignored) {}
        try {
            Timestamp c = rs.getTimestamp("created_at");
            if (c != null) tc.setCreatedAt(c.toLocalDateTime());
            Timestamp u = rs.getTimestamp("updated_at");
            if (u != null) tc.setUpdatedAt(u.toLocalDateTime());
        } catch (SQLException ignored) {}
        return tc;
    }
}
