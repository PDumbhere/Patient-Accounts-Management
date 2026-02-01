package com.nirwan.dentalclinic.security;

import com.nirwan.dentalclinic.models.User;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime lastLoginTime;
    private static final String SESSION_FILE = "session.dat";
    
    private SessionManager() {}
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Starts a new session for the user
     */
    public void startSession(User user) {
        this.currentUser = user;
        this.lastLoginTime = LocalDateTime.now();
        saveSessionToFile();
    }
    
    /**
     * Ends the current session
     */
    public void endSession() {
        this.currentUser = null;
        this.lastLoginTime = null;
        deleteSessionFile();
    }
    
    /**
     * Checks if user is currently logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Checks if user needs to login again (new day)
     */
    public boolean requiresNewLogin() {
        if (!isLoggedIn()) {
            return true;
        }
        
        // Check if last login was on a different day
        return lastLoginTime != null && !lastLoginTime.toLocalDate().equals(LocalDate.now());
    }
    
    /**
     * Gets the current logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Gets the last login time
     */
    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }
    
    /**
     * Saves session to file for persistence
     */
    private void saveSessionToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(SESSION_FILE))) {
            oos.writeObject(currentUser);
            oos.writeObject(lastLoginTime);
        } catch (IOException e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }
    
    /**
     * Loads session from file
     */
    public void loadSessionFromFile() {
        File sessionFile = new File(SESSION_FILE);
        if (!sessionFile.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(SESSION_FILE))) {
            User user = (User) ois.readObject();
            LocalDateTime loginTime = (LocalDateTime) ois.readObject();
            
            // Only restore session if it's from today
            if (loginTime != null && loginTime.toLocalDate().equals(LocalDate.now())) {
                this.currentUser = user;
                this.lastLoginTime = loginTime;
            } else {
                // Session is from a previous day, delete it
                deleteSessionFile();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load session: " + e.getMessage());
            deleteSessionFile();
        }
    }
    
    /**
     * Deletes the session file
     */
    private void deleteSessionFile() {
        File sessionFile = new File(SESSION_FILE);
        if (sessionFile.exists()) {
            sessionFile.delete();
        }
    }
    
    /**
     * Updates the last login time to current time
     */
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
        saveSessionToFile();
    }
}
