-- Insert organization
INSERT INTO organizations (id, name, plan_type, max_employees) 
VALUES 
  ('550e8400-e29b-41d4-a716-446655440000', 'Demo Corp', 'pro', 100);

-- Insert admin user
INSERT INTO users (id, email, password_hash, full_name, role, organization_id, is_active)
VALUES 
  ('550e8400-e29b-41d4-a716-446655440001', 
   'admin@demo.com', 
   '$2a$10$X4qIqO7gYN9Y8Kj5zFLNJeE7Sy.jZEMxH8Oc1wKxiYXNEkDOX8C6q', -- password: Admin123!
   'Admin User',
   'admin',
   '550e8400-e29b-41d4-a716-446655440000',
   true);

-- Insert locations
INSERT INTO locations (id, organization_id, name, address, latitude, longitude, radius)
VALUES 
  ('550e8400-e29b-41d4-a716-446655440010',
   '550e8400-e29b-41d4-a716-446655440000',
   'Main Office',
   'Jl. Griya JM 2 No.21, Jakarta',
   -6.2984105,
   106.964303,
   100),
  ('550e8400-e29b-41d4-a716-446655440011',
   '550e8400-e29b-41d4-a716-446655440000',
   'Branch Office',
   'Jl. Sudirman No.45, Jakarta',
   -6.2088,
   106.8456,
   150);

-- Insert work schedule
INSERT INTO work_schedules (user_id, day_of_week, start_time, end_time)
SELECT 
  '550e8400-e29b-41d4-a716-446655440001',
  generate_series(1, 5), -- Monday to Friday
  '08:00:00',
  '17:00:00';