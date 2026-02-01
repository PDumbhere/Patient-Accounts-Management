package com.nirwan.dentalclinic.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Hashes a password with SHA-256 and a salt
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes());
            byte[] hashedBytes = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
    
    /**
     * Generates a random salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Verifies a password against the stored hash
     */
    public static boolean verifyPassword(String password, String salt, String hashedPassword) {
        String computedHash = hashPassword(password, salt);
        return computedHash.equals(hashedPassword);
    }
    
    /**
     * Creates a complete password hash (salt + hash combined)
     */
    public static String createPasswordHash(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return salt + ":" + hash;
    }
    
    /**
     * Extracts salt from combined password hash
     */
    public static String extractSalt(String passwordHash) {
        String[] parts = passwordHash.split(":");
        return parts.length >= 2 ? parts[0] : "";
    }
    
    /**
     * Extracts hash from combined password hash
     */
    public static String extractHash(String passwordHash) {
        String[] parts = passwordHash.split(":");
        return parts.length >= 2 ? parts[1] : passwordHash;
    }
    
    /**
     * Verifies password using combined hash format
     */
    public static boolean verifyPasswordHash(String password, String combinedHash) {
        String salt = extractSalt(combinedHash);
        String hash = extractHash(combinedHash);
        return verifyPassword(password, salt, hash);
    }
}
