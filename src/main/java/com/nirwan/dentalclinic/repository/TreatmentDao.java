package com.nirwan.dentalclinic.repository;


import com.nirwan.dentalclinic.models.Treatment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TreatmentDao {
    private static final String URL = "jdbc:h2:./database/dentalclinic";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // CREATE a new treatment
    public static void addTreatment(Treatment treatment) {
        String query = "INSERT INTO Treatment (name, cost) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, treatment.getName());
            pstmt.setDouble(2, treatment.getCost());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding treatment: " + e.getMessage());
        }
    }

    // READ all treatments
    public static List<Treatment> getAllTreatments() {
        List<Treatment> treatments = new ArrayList<>();
        String query = "SELECT * FROM Treatment";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                treatments.add(new Treatment(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("cost")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching treatments: " + e.getMessage());
        }
        return treatments;
    }

    // UPDATE a treatment
    public static void updateTreatment(Treatment treatment) {
        String query = "UPDATE Treatment SET name = ?, cost = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, treatment.getName());
            pstmt.setDouble(2, treatment.getCost());
            pstmt.setInt(3, treatment.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating treatment: " + e.getMessage());
        }
    }

    // DELETE a treatment
    public static void deleteTreatment(int id) {
        String query = "DELETE FROM Treatment WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting treatment: " + e.getMessage());
        }
    }
}
