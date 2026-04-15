-- V3: Bulk seed 50 operators and 500 plans

-- Seed 46 dummy operators (4 exist in V2)
INSERT IGNORE INTO operators (name, code, category, logo_url, is_active) VALUES
('Mock Operator 5', 'MOCK5', 'PREPAID', null, 1),
('Mock Operator 6', 'MOCK6', 'PREPAID', null, 1),
('Mock Operator 7', 'MOCK7', 'PREPAID', null, 1),
('Mock Operator 8', 'MOCK8', 'PREPAID', null, 1),
('Mock Operator 9', 'MOCK9', 'PREPAID', null, 1),
('Mock Operator 10', 'MOCK10', 'PREPAID', null, 1),
('Mock Operator 11', 'MOCK11', 'PREPAID', null, 1),
('Mock Operator 12', 'MOCK12', 'PREPAID', null, 1),
('Mock Operator 13', 'MOCK13', 'PREPAID', null, 1),
('Mock Operator 14', 'MOCK14', 'PREPAID', null, 1),
('Mock Operator 15', 'MOCK15', 'PREPAID', null, 1),
('Mock Operator 16', 'MOCK16', 'PREPAID', null, 1),
('Mock Operator 17', 'MOCK17', 'PREPAID', null, 1),
('Mock Operator 18', 'MOCK18', 'PREPAID', null, 1),
('Mock Operator 19', 'MOCK19', 'PREPAID', null, 1),
('Mock Operator 20', 'MOCK20', 'PREPAID', null, 1),
('Mock Operator 21', 'MOCK21', 'PREPAID', null, 1),
('Mock Operator 22', 'MOCK22', 'PREPAID', null, 1),
('Mock Operator 23', 'MOCK23', 'PREPAID', null, 1),
('Mock Operator 24', 'MOCK24', 'PREPAID', null, 1),
('Mock Operator 25', 'MOCK25', 'PREPAID', null, 1),
('Mock Operator 26', 'MOCK26', 'PREPAID', null, 1),
('Mock Operator 27', 'MOCK27', 'PREPAID', null, 1),
('Mock Operator 28', 'MOCK28', 'PREPAID', null, 1),
('Mock Operator 29', 'MOCK29', 'PREPAID', null, 1),
('Mock Operator 30', 'MOCK30', 'PREPAID', null, 1),
('Mock Operator 31', 'MOCK31', 'PREPAID', null, 1),
('Mock Operator 32', 'MOCK32', 'PREPAID', null, 1),
('Mock Operator 33', 'MOCK33', 'PREPAID', null, 1),
('Mock Operator 34', 'MOCK34', 'PREPAID', null, 1),
('Mock Operator 35', 'MOCK35', 'PREPAID', null, 1),
('Mock Operator 36', 'MOCK36', 'PREPAID', null, 1),
('Mock Operator 37', 'MOCK37', 'PREPAID', null, 1),
('Mock Operator 38', 'MOCK38', 'PREPAID', null, 1),
('Mock Operator 39', 'MOCK39', 'PREPAID', null, 1),
('Mock Operator 40', 'MOCK40', 'PREPAID', null, 1),
('Mock Operator 41', 'MOCK41', 'PREPAID', null, 1),
('Mock Operator 42', 'MOCK42', 'PREPAID', null, 1),
('Mock Operator 43', 'MOCK43', 'PREPAID', null, 1),
('Mock Operator 44', 'MOCK44', 'PREPAID', null, 1),
('Mock Operator 45', 'MOCK45', 'PREPAID', null, 1),
('Mock Operator 46', 'MOCK46', 'PREPAID', null, 1),
('Mock Operator 47', 'MOCK47', 'PREPAID', null, 1),
('Mock Operator 48', 'MOCK48', 'PREPAID', null, 1),
('Mock Operator 49', 'MOCK49', 'PREPAID', null, 1),
('Mock Operator 50', 'MOCK50', 'PREPAID', null, 1);

-- Seed 10 plans per dummy operator
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, CONCAT('Bulk Plan ', code, '-', numbers.n), 100 + numbers.n * 10, 28, '1GB/day', 'Unlimited', '100 SMS/day', NULL, 'DATA', 1, 0
FROM operators
JOIN (SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) numbers
WHERE code LIKE 'MOCK%';
