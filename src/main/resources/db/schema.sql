-- Enable foreign key constraints
PRAGMA foreign_keys = ON;

-- Create treatment table
CREATE TABLE IF NOT EXISTS treatment (
    treatment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL,
    description TEXT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    amount_paid DECIMAL(10, 2) DEFAULT 0.00,
    amount_pending DECIMAL(10, 2) GENERATED ALWAYS AS (total_amount - amount_paid) STORED,
    is_active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);

-- Create treatment_cost table
CREATE TABLE IF NOT EXISTS treatment_cost (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    treatment_id INTEGER NOT NULL,
    cost DECIMAL(10, 2) NOT NULL,
    status TEXT NOT NULL, -- 'active' or 'inactive'
    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id) ON DELETE CASCADE
);

-- Create payment table
CREATE TABLE IF NOT EXISTS payment (
    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    treatment_id INTEGER NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method TEXT NOT NULL,
    notes TEXT,
    FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id) ON DELETE CASCADE
);

-- Create trigger to update treatment's updated_at timestamp
CREATE TRIGGER IF NOT EXISTS update_treatment_timestamp
AFTER UPDATE ON treatment
BEGIN
    UPDATE treatment 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE treatment_id = NEW.treatment_id;
END;

-- Create trigger to update treatment cost status when new cost is added
CREATE TRIGGER IF NOT EXISTS update_treatment_cost_status
AFTER INSERT ON treatment_cost
BEGIN
    -- Set all previous costs as inactive
    UPDATE treatment_cost 
    SET status = 'inactive' 
    WHERE treatment_id = NEW.treatment_id 
    AND id != NEW.id;
    
    -- Update treatment total amount
    UPDATE treatment 
    SET total_amount = (
        SELECT SUM(cost) 
        FROM treatment_cost 
        WHERE treatment_id = NEW.treatment_id
    )
    WHERE treatment_id = NEW.treatment_id;
END;
