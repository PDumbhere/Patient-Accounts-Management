package com.nirwan.dentalclinic.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/patient-accounts-db";
    private static final String USER = "patient-accounts-user";
    private static final String PASSWORD = "patient-accounts-user";
    private static DatabaseConnection instance;
    private final BasicDataSource dataSource;

    private DatabaseConnection() {
        dataSource = new BasicDataSource();
        dataSource.setUrl(URL);
        dataSource.setUsername(USER);
        dataSource.setPassword(PASSWORD);
        dataSource.setMinIdle(5);
        dataSource.setMaxIdle(10);
        dataSource.setMaxOpenPreparedStatements(100);
    }

    public static void initializeDatabase() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Initialize the connection pool
            instance = new DatabaseConnection();
            
            // Test the connection
            try (Connection conn = instance.getConnection()) {
                System.out.println("Connected to MySQL Database successfully.");
                instance.createTables();
                System.out.println("DB initialized successfully");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static DatabaseConnection getInstance() {
        return instance;
    }

    public static void createTableIfNotExists(String createTableSQL) {
        try {
            try (var stmt = instance.dataSource.getConnection().createStatement()) {
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            System.err.println("Failed to create table " + ": " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                System.err.println("Failed to close connection pool: " + e.getMessage());
            }
        }
    }

    public void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Save the current auto-commit state
            boolean initialAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                // Create Patient table
                String patientTableSQL = "CREATE TABLE IF NOT EXISTS Patient (\n" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    name VARCHAR(255) UNIQUE NOT NULL,\n" +
                        "    is_deleted BOOLEAN DEFAULT FALSE,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n" +
                        ")";
                try (var stmt = conn.createStatement()) {
                    stmt.execute(patientTableSQL);
                }

                // Create Treatment table
                String treatmentTableSQL = "CREATE TABLE IF NOT EXISTS Treatment (\n" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    treatment_id VARCHAR(255) UNIQUE NOT NULL,\n" +
                        "    patient_id INT NOT NULL,\n" +
                        "    description TEXT,\n" +
                        "    total_amount DOUBLE NOT NULL,\n" +
                        "    amount_paid DOUBLE DEFAULT 0.0,\n" +
                        "    amount_pending DOUBLE GENERATED ALWAYS AS (total_amount - amount_paid) STORED,\n" +
                        "    is_deleted BOOLEAN DEFAULT FALSE,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
                        "    is_active BOOLEAN DEFAULT TRUE,\n" +
                        "    FOREIGN KEY (patient_id) REFERENCES Patient(id) ON DELETE CASCADE\n" +
                        ")";
                try (var stmt = conn.createStatement()) {
                    stmt.execute(treatmentTableSQL);
                }

                // Create TreatmentCost table to track cost history and status
                String treatmentCostTableSQL = "CREATE TABLE IF NOT EXISTS TreatmentCost (\n" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    treatment_id VARCHAR(255) NOT NULL,\n" +
                        "    cost DOUBLE NOT NULL,\n" +
                        "    status ENUM('PENDING', 'PARTIALLY_PAID', 'PAID') NOT NULL,\n" +
                        "    notes TEXT,\n" +
                        "    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    is_deleted BOOLEAN DEFAULT FALSE,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
                        "    FOREIGN KEY (treatment_id) REFERENCES Treatment(treatment_id) ON DELETE CASCADE\n" +
                        ")";
                try (var stmt = conn.createStatement()) {
                    stmt.execute(treatmentCostTableSQL);
                }

                // Create Payment table to track payments
                String paymentTableSQL = "CREATE TABLE IF NOT EXISTS Payment (\n" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    treatment_id VARCHAR(255) NOT NULL,\n" +
                        "    amount DOUBLE NOT NULL,\n" +
                        "    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    payment_method ENUM('CASH', 'CARD', 'UPI', 'BANK_TRANSFER') NOT NULL,\n" +
                        "    transaction_reference VARCHAR(255),\n" +
                        "    notes TEXT,\n" +
                        "    is_deleted BOOLEAN DEFAULT FALSE,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
                        "    FOREIGN KEY (treatment_id) REFERENCES Treatment(treatment_id) ON DELETE CASCADE\n" +
                        ")";
                try (var stmt = conn.createStatement()) {
                    stmt.execute(paymentTableSQL);
                }

                // Create a simplified view that shows patient information with their treatments
                String viewSql = "CREATE OR REPLACE VIEW PatientList AS " +
                        "SELECT " +
                        "    p.id as patientId, " +
                        "    p.name as patientName, " +
                        "    t.id as treatmentId, " +
                        "    t.treatment_id as treatmentReference, " +
                        "    t.description as treatmentDescription, " +
                        "    t.total_amount as totalAmount, " +
                        "    t.amount_paid as amountPaid, " +
                        "    t.amount_pending as balancePayment, " +
                        "    t.created_at as treatmentDate, " +
                        "    p.created_at as patientSince " +
                        "FROM Patient p " +
                        "LEFT JOIN Treatment t ON p.id = t.patient_id " +
                        "WHERE p.is_deleted = FALSE AND (t.is_deleted = FALSE OR t.id IS NULL)";

                try (var stmt = conn.createStatement()) {
                    stmt.execute("DROP VIEW IF EXISTS PatientList");
                    stmt.execute(viewSql);
                } catch (SQLException e) {
                    System.err.println("Failed to create PatientList view: " + e.getMessage());
                    throw e;
                }

                // If we get here, commit all changes
                conn.commit();
            } catch (SQLException e) {
                // If there's an error, rollback the transaction
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
                throw e;
            } finally {
                // Restore the original auto-commit state
                try {
                    conn.setAutoCommit(initialAutoCommit);
                } catch (SQLException e) {
                    System.err.println("Failed to restore auto-commit state: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to create tables: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }
}
