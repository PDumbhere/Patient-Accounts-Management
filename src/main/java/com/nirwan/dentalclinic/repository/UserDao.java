package com.nirwan.dentalclinic.repository;

import com.nirwan.dentalclinic.models.User;
import com.nirwan.dentalclinic.database.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDao {
    
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM User WHERE username = ? AND is_deleted = FALSE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM User WHERE id = ? AND is_deleted = FALSE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public boolean save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }
    
    private boolean insert(User user) {
        String sql = "INSERT INTO User (username, password_hash, full_name, role, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getRole());
            stmt.setBoolean(5, user.isActive());
            
            // Handle null timestamps safely
            if (user.getCreatedAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(user.getCreatedAt()));
            } else {
                stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            if (user.getUpdatedAt() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(user.getUpdatedAt()));
            } else {
                stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting user: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean update(User user) {
        String sql = "UPDATE User SET username = ?, password_hash = ?, full_name = ?, role = ?, " +
                    "last_login = ?, is_active = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getRole());
            
            if (user.getLastLogin() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(user.getLastLogin()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }
            
            stmt.setBoolean(6, user.isActive());
            
            // Always update updated_at to current time
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(8, user.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean updateLastLogin(Long userId) {
        String sql = "UPDATE User SET last_login = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setTimestamp(1, Timestamp.valueOf(now));
            stmt.setTimestamp(2, Timestamp.valueOf(now));
            stmt.setLong(3, userId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
        
        return false;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        user.setActive(rs.getBoolean("is_active"));
        
        return user;
    }
}
