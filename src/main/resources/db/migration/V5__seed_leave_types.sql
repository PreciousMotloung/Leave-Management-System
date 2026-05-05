-- Insert default leave types
INSERT INTO leave_types (name, default_days, description) VALUES
('ANNUAL_LEAVE', 21, 'Annual leave for rest and recreation'),
('SICK_LEAVE', 10, 'Leave for illness or medical appointments'),
('FAMILY_RESPONSIBILITY', 3, 'Leave for family responsibilities and emergencies');
