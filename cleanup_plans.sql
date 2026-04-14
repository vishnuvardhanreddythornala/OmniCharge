-- Remove the duplicate plans inserted by the failed script
DELETE FROM plans WHERE id > 7;

-- Re-insert cleanly with the fixed enums
-- Seed Plans for Jio
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Jio 299 Unlimited', 299.00, 28, '2GB/day', 'Unlimited', '100 SMS/day', 'JioTV, JioCinema', 'UNLIMITED', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'JIO';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Jio 666 Magic', 666.00, 84, '1.5GB/day', 'Unlimited', '100 SMS/day', '5G Unlimited Data', 'RECOMMENDED', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'JIO';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Jio Data Booster 19', 19.00, 1, '1GB', 'None', 'None', 'Active Plan Required', 'DATA', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'JIO';

-- Seed Plans for Airtel
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Airtel 299 Hero', 299.00, 28, '1.5GB/day', 'Unlimited', '100 SMS/day', 'Wynk Music', 'RECOMMENDED', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'AIRTEL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Airtel 719 Epic', 719.00, 84, '2.0GB/day', 'Unlimited', '100 SMS/day', 'Disney+ Hotstar', 'UNLIMITED', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'AIRTEL';

-- Seed Plans for VI
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Vi 299 Binge All Night', 299.00, 28, '1.5GB/day', 'Unlimited', '100 SMS/day', 'Binge All Night', 'UNLIMITED', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'VI';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'Vi 479 Hero', 479.00, 56, '1.5GB/day', 'Unlimited', '100 SMS/day', 'Binge All Night', 'RECOMMENDED', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'VI';

-- Seed Plans for BSNL
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator, created_date, last_modified_date, created_by, last_modified_by)
SELECT id, 'BSNL 153 Special', 153.00, 26, '1GB/day', 'Unlimited', '100 SMS/day', 'PRBT', 'TALKTIME', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM' FROM operators WHERE code = 'BSNL';
