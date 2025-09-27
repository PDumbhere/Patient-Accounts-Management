package com.nirwan.dentalclinic.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:h2:file:C:/Nirwan/db/accounts-application;DB_CLOSE_ON_EXIT=FALSE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private Connection connection;
    private static DatabaseConnection instance;

    private DatabaseConnection(Connection connection) {
        this.connection = connection;
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to H2 Database successfully.");
            instance= new DatabaseConnection(conn);
            instance.createTables();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static DatabaseConnection getInstance() {
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
    public void createTables() {
        try{
            String table1SQL = "CREATE TABLE IF NOT EXISTS Patient (\n" +
                    "    id INT PRIMARY KEY,\n" +
                    "    name VARCHAR(255) UNIQUE,\n" +
                    "    age INT,\n" +
                    "    mobile VARCHAR(255),\n" +
                    "    gender VARCHAR(255)\n" +
                    ")";

            createTableIfNotExists(table1SQL);

            String table2SQL = "CREATE TABLE IF NOT EXISTS Treatment (\n" +
                    "    id INT PRIMARY KEY,\n" +
                    "    name VARCHAR(255) UNIQUE,\n" +
                    "    cost DOUBLE,\n" +
                    "    remainingPayment DOUBLE\n" +
                    ")";

            createTableIfNotExists(table2SQL);

            String table3SQL = "CREATE TABLE IF NOT EXISTS Patient_Treatment_Mapping (\n" +
                    "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                    "    patient_id INT,\n" +
                    "    treatment_id INT,\n" +
                    "    status ENUM('ongoing', 'completed', 'abandoned'),\n" +
                    "    balance_payment DOUBLE DEFAULT 0.0,\n" +
                    "    FOREIGN KEY (patient_id) REFERENCES Patient(id) ON DELETE CASCADE,\n" +
                    "    FOREIGN KEY (treatment_id) REFERENCES Treatment(id) ON DELETE CASCADE\n" +
                    ");\n";

            createTableIfNotExists(table3SQL);

            String table4SQL = "CREATE TABLE IF NOT EXISTS Appointment (\n" +
                    "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                    "    patient_treatment_mapping_id INT,\n" +
                    "    appointment_date DATE,\n" +
                    "    payment_made DOUBLE,\n" +
                    "    remarks TEXT,\n" +
                    "    FOREIGN KEY (patient_treatment_mapping_id) REFERENCES Patient_Treatment_Mapping(id) ON DELETE CASCADE\n" +
                    ")";

            createTableIfNotExists(table4SQL);

            String viewSql = "CREATE VIEW IF NOT EXISTS PatientList AS " +
                    "SELECT " +
                    "    p.name AS patientName, " +
                    "    p.age, " +
                    "    t.name AS latestTreatment, " +
                    "    a.appointment_date AS latestTreatmentDate, " +
                    "    ptm.balance_payment AS balancePayment " +
                    "FROM patient p " +
                    "JOIN Patient_Treatment_Mapping ptm ON p.id = ptm.patient_id " +
                    "JOIN Treatment t ON ptm.treatment_id = t.id " +
                    "JOIN Appointment a ON ptm.id = a.patient_treatment_mapping_id " +
                    "WHERE a.appointment_date = ( " +
                    "    SELECT MAX(a2.appointment_date) " +
                    "    FROM Appointment a2 " +
                    "    JOIN Patient_Treatment_Mapping ptm2 ON a2.patient_treatment_mapping_id = ptm2.id " +
                    "    WHERE ptm2.patient_id = p.id " +
                    ")";

            createTableIfNotExists(viewSql);

            instance.connection.commit();
        }catch (SQLException e) {
            System.err.println("Failed to create tables: " + e.getMessage());
        }

    }

    public static void createTableIfNotExists(String createTableSQL) {
        try {
            try (var stmt = instance.connection.createStatement()) {
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            System.err.println("Failed to create table " +  ": " + e.getMessage());
        }
    }
}
