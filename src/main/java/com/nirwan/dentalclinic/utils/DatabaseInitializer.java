package com.nirwan.dentalclinic.utils;

import com.nirwan.dentalclinic.models.User;
import com.nirwan.dentalclinic.repository.UserDao;
import com.nirwan.dentalclinic.security.SecurityUtils;

import java.time.LocalDateTime;

/**
 * Utility class to initialize default users in the database
 */
public class DatabaseInitializer {
    
    /**
     * Creates default admin user if it doesn't exist
     */
    public static void createDefaultAdminUser() {
        UserDao userDao = new UserDao();
        
        // Check if admin user already exists
        if (userDao.findByUsername("admin").isPresent()) {
            System.out.println("Admin user already exists");
            return;
        }
        
        // Create default admin user
        String defaultPassword = "admin123"; // Change this in production!
        String passwordHash = SecurityUtils.createPasswordHash(defaultPassword);
        
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPasswordHash(passwordHash);
        adminUser.setFullName("System Administrator");
        adminUser.setRole("ADMIN");
        adminUser.setActive(true);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        
        if (userDao.save(adminUser)) {
            System.out.println("Default admin user created successfully");
            System.out.println("Username: admin");
            System.out.println("Password: " + defaultPassword);
            System.out.println("IMPORTANT: Change this password in production!");
        } else {
            System.err.println("Failed to create default admin user");
        }
    }
    
    /**
     * Creates a sample user for testing
     */
    public static void createSampleUser() {
        UserDao userDao = new UserDao();
        
        // Check if sample user already exists
        if (userDao.findByUsername("user").isPresent()) {
            System.out.println("Sample user already exists");
            return;
        }
        
        // Create sample user
        String defaultPassword = "user123";
        String passwordHash = SecurityUtils.createPasswordHash(defaultPassword);
        
        User sampleUser = new User();
        sampleUser.setUsername("user");
        sampleUser.setPasswordHash(passwordHash);
        sampleUser.setFullName("Sample User");
        sampleUser.setRole("USER");
        sampleUser.setActive(true);
        sampleUser.setCreatedAt(LocalDateTime.now());
        sampleUser.setUpdatedAt(LocalDateTime.now());
        
        if (userDao.save(sampleUser)) {
            System.out.println("Sample user created successfully");
            System.out.println("Username: user");
            System.out.println("Password: " + defaultPassword);
        } else {
            System.err.println("Failed to create sample user");
        }
    }
}
