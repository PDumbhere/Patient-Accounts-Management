-- Add is_active column to Treatment table
ALTER TABLE Treatment ADD COLUMN is_active BOOLEAN DEFAULT TRUE;

-- Update existing records to be active by default
UPDATE Treatment SET is_active = TRUE;
