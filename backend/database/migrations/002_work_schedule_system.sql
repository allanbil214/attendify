-- ============================================================================
-- Work Schedule System - Database Migration
-- File: database/migrations/002_work_schedule_system.sql
-- ============================================================================

-- Step 1: Add employee_type to users table
ALTER TABLE users 
ADD COLUMN employee_type VARCHAR(20) DEFAULT 'fixed' 
CHECK (employee_type IN ('fixed', 'flexible', 'field_worker'));

-- Add comment for clarity
COMMENT ON COLUMN users.employee_type IS 'fixed: Has fixed schedule | flexible: Flexible hours | field_worker: No fixed schedule';

-- Step 2: Update organizations table to include default work hours
ALTER TABLE organizations
ADD COLUMN default_work_hours JSONB DEFAULT '{
  "monday": {"start": "08:00", "end": "17:00", "is_working_day": true},
  "tuesday": {"start": "08:00", "end": "17:00", "is_working_day": true},
  "wednesday": {"start": "08:00", "end": "17:00", "is_working_day": true},
  "thursday": {"start": "08:00", "end": "17:00", "is_working_day": true},
  "friday": {"start": "08:00", "end": "17:00", "is_working_day": true},
  "saturday": {"start": "08:00", "end": "17:00", "is_working_day": false},
  "sunday": {"start": "08:00", "end": "17:00", "is_working_day": false}
}'::JSONB,
ADD COLUMN late_threshold_minutes INTEGER DEFAULT 15;

-- Add comments
COMMENT ON COLUMN organizations.default_work_hours IS 'Default work schedule for all employees in the organization';
COMMENT ON COLUMN organizations.late_threshold_minutes IS 'Minutes after start time before employee is marked as late';

-- Step 3: Enhance work_schedules table
-- Drop existing table if it doesn't have data, or alter if it does
DROP TABLE IF EXISTS work_schedules;

CREATE TABLE work_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_working_day BOOLEAN DEFAULT true,
    break_duration_minutes INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, day_of_week)
);

COMMENT ON COLUMN work_schedules.day_of_week IS '0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday';
COMMENT ON COLUMN work_schedules.break_duration_minutes IS 'Total break time in minutes (lunch, etc)';

-- Step 4: Create schedule templates table (for quick assignment)
CREATE TABLE schedule_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    schedule_data JSONB NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE schedule_templates IS 'Reusable schedule templates like "Morning Shift", "Night Shift", etc.';
COMMENT ON COLUMN schedule_templates.schedule_data IS 'JSON array of schedule entries for each day';

-- Example schedule_data format:
-- [
--   {"day": 1, "start": "09:00", "end": "17:00", "is_working_day": true},
--   {"day": 2, "start": "09:00", "end": "17:00", "is_working_day": true},
--   ...
-- ]

-- Step 5: Create indexes for performance
CREATE INDEX idx_work_schedules_user ON work_schedules(user_id);
CREATE INDEX idx_work_schedules_day ON work_schedules(user_id, day_of_week);
CREATE INDEX idx_schedule_templates_org ON schedule_templates(organization_id);
CREATE INDEX idx_users_employee_type ON users(employee_type);

-- Step 6: Create trigger for work_schedules updated_at
CREATE TRIGGER update_work_schedules_updated_at BEFORE UPDATE ON work_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedule_templates_updated_at BEFORE UPDATE ON schedule_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Step 7: Seed default schedule templates
INSERT INTO schedule_templates (organization_id, name, description, schedule_data)
SELECT 
    id,
    'Standard 9-5',
    'Monday to Friday, 9 AM to 5 PM',
    '[
        {"day": 1, "start": "09:00", "end": "17:00", "is_working_day": true, "break_duration_minutes": 60},
        {"day": 2, "start": "09:00", "end": "17:00", "is_working_day": true, "break_duration_minutes": 60},
        {"day": 3, "start": "09:00", "end": "17:00", "is_working_day": true, "break_duration_minutes": 60},
        {"day": 4, "start": "09:00", "end": "17:00", "is_working_day": true, "break_duration_minutes": 60},
        {"day": 5, "start": "09:00", "end": "17:00", "is_working_day": true, "break_duration_minutes": 60},
        {"day": 6, "start": "09:00", "end": "17:00", "is_working_day": false, "break_duration_minutes": 0},
        {"day": 0, "start": "09:00", "end": "17:00", "is_working_day": false, "break_duration_minutes": 0}
    ]'::JSONB
FROM organizations;

INSERT INTO schedule_templates (organization_id, name, description, schedule_data)
SELECT 
    id,
    'Morning Shift',
    'Monday to Friday, 6 AM to 2 PM',
    '[
        {"day": 1, "start": "06:00", "end": "14:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 2, "start": "06:00", "end": "14:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 3, "start": "06:00", "end": "14:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 4, "start": "06:00", "end": "14:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 5, "start": "06:00", "end": "14:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 6, "start": "06:00", "end": "14:00", "is_working_day": false, "break_duration_minutes": 0},
        {"day": 0, "start": "06:00", "end": "14:00", "is_working_day": false, "break_duration_minutes": 0}
    ]'::JSONB
FROM organizations;

INSERT INTO schedule_templates (organization_id, name, description, schedule_data)
SELECT 
    id,
    'Afternoon Shift',
    'Monday to Friday, 2 PM to 10 PM',
    '[
        {"day": 1, "start": "14:00", "end": "22:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 2, "start": "14:00", "end": "22:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 3, "start": "14:00", "end": "22:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 4, "start": "14:00", "end": "22:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 5, "start": "14:00", "end": "22:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 6, "start": "14:00", "end": "22:00", "is_working_day": false, "break_duration_minutes": 0},
        {"day": 0, "start": "14:00", "end": "22:00", "is_working_day": false, "break_duration_minutes": 0}
    ]'::JSONB
FROM organizations;

INSERT INTO schedule_templates (organization_id, name, description, schedule_data)
SELECT 
    id,
    'Night Shift',
    'Monday to Friday, 10 PM to 6 AM',
    '[
        {"day": 1, "start": "22:00", "end": "06:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 2, "start": "22:00", "end": "06:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 3, "start": "22:00", "end": "06:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 4, "start": "22:00", "end": "06:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 5, "start": "22:00", "end": "06:00", "is_working_day": true, "break_duration_minutes": 30},
        {"day": 6, "start": "22:00", "end": "06:00", "is_working_day": false, "break_duration_minutes": 0},
        {"day": 0, "start": "22:00", "end": "06:00", "is_working_day": false, "break_duration_minutes": 0}
    ]'::JSONB
FROM organizations;

-- Step 8: Create helper function to get user's schedule for a specific day
CREATE OR REPLACE FUNCTION get_user_schedule(p_user_id UUID, p_day_of_week INTEGER)
RETURNS TABLE (
    start_time TIME,
    end_time TIME,
    is_working_day BOOLEAN,
    break_duration_minutes INTEGER
) AS $$
BEGIN
    -- First, try to get custom schedule
    RETURN QUERY
    SELECT ws.start_time, ws.end_time, ws.is_working_day, ws.break_duration_minutes
    FROM work_schedules ws
    WHERE ws.user_id = p_user_id 
      AND ws.day_of_week = p_day_of_week
      AND ws.is_active = true;
    
    -- If no custom schedule found, return organization default
    IF NOT FOUND THEN
        RETURN QUERY
        SELECT 
            (org.default_work_hours -> CASE p_day_of_week
                WHEN 0 THEN 'sunday'
                WHEN 1 THEN 'monday'
                WHEN 2 THEN 'tuesday'
                WHEN 3 THEN 'wednesday'
                WHEN 4 THEN 'thursday'
                WHEN 5 THEN 'friday'
                WHEN 6 THEN 'saturday'
            END ->> 'start')::TIME as start_time,
            (org.default_work_hours -> CASE p_day_of_week
                WHEN 0 THEN 'sunday'
                WHEN 1 THEN 'monday'
                WHEN 2 THEN 'tuesday'
                WHEN 3 THEN 'wednesday'
                WHEN 4 THEN 'thursday'
                WHEN 5 THEN 'friday'
                WHEN 6 THEN 'saturday'
            END ->> 'end')::TIME as end_time,
            (org.default_work_hours -> CASE p_day_of_week
                WHEN 0 THEN 'sunday'
                WHEN 1 THEN 'monday'
                WHEN 2 THEN 'tuesday'
                WHEN 3 THEN 'wednesday'
                WHEN 4 THEN 'thursday'
                WHEN 5 THEN 'friday'
                WHEN 6 THEN 'saturday'
            END ->> 'is_working_day')::BOOLEAN as is_working_day,
            0 as break_duration_minutes
        FROM users u
        JOIN organizations org ON u.organization_id = org.id
        WHERE u.id = p_user_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Step 9: Create function to check if employee is late
CREATE OR REPLACE FUNCTION is_employee_late(
    p_user_id UUID,
    p_check_in_time TIMESTAMP
)
RETURNS BOOLEAN AS $$
DECLARE
    v_employee_type VARCHAR(20);
    v_day_of_week INTEGER;
    v_check_in_time TIME;
    v_start_time TIME;
    v_late_threshold INTEGER;
    v_is_working_day BOOLEAN;
BEGIN
    -- Get employee type
    SELECT employee_type INTO v_employee_type
    FROM users
    WHERE id = p_user_id;
    
    -- Flexible and field workers are never late
    IF v_employee_type IN ('flexible', 'field_worker') THEN
        RETURN false;
    END IF;
    
    -- Get day of week (0=Sunday, 6=Saturday)
    v_day_of_week := EXTRACT(DOW FROM p_check_in_time);
    v_check_in_time := p_check_in_time::TIME;
    
    -- Get schedule for this day
    SELECT start_time, is_working_day 
    INTO v_start_time, v_is_working_day
    FROM get_user_schedule(p_user_id, v_day_of_week);
    
    -- If not a working day, not late
    IF NOT v_is_working_day THEN
        RETURN false;
    END IF;
    
    -- Get late threshold from organization
    SELECT late_threshold_minutes INTO v_late_threshold
    FROM users u
    JOIN organizations o ON u.organization_id = o.id
    WHERE u.id = p_user_id;
    
    -- Add threshold to start time
    v_start_time := v_start_time + (v_late_threshold || ' minutes')::INTERVAL;
    
    -- Check if late
    RETURN v_check_in_time > v_start_time;
END;
$$ LANGUAGE plpgsql;

-- Step 10: Update existing users to have default employee_type
UPDATE users SET employee_type = 'fixed' WHERE employee_type IS NULL;

-- Verification queries
-- Run these to verify the migration worked correctly

-- Check organization default hours
-- SELECT name, default_work_hours, late_threshold_minutes FROM organizations;

-- Check schedule templates
-- SELECT name, description FROM schedule_templates;

-- Check users with employee types
-- SELECT full_name, employee_type FROM users;

-- Test the helper functions
-- SELECT * FROM get_user_schedule('user-uuid-here', 1); -- Monday
-- SELECT is_employee_late('user-uuid-here', NOW()); -- Check if late now