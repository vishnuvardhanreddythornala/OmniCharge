-- Seed Operator Data with Logos
-- Ensure we have unique codes
INSERT IGNORE INTO operators (name, code, category, logo_url, is_active) VALUES
('Reliance Jio', 'JIO', 'PREPAID', 'https://upload.wikimedia.org/wikipedia/commons/4/43/Jio_logo_%282021%29.svg', 1),
('Airtel', 'AIRTEL', 'PREPAID', 'https://upload.wikimedia.org/wikipedia/commons/e/e0/Airtel_logo.svg', 1),
('Vodafone Idea', 'VI', 'PREPAID', 'https://upload.wikimedia.org/wikipedia/commons/2/27/Vi_logo.svg', 1),
('BSNL', 'BSNL', 'PREPAID', 'https://upload.wikimedia.org/wikipedia/commons/c/c5/BSNL_logo.svg', 1);

-- Seed Plans for Jio (Assuming JIO was ID 1 if inserted fresh, but using subqueries to be safe)
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Jio 299 Unlimited', 299.00, 28, '2GB/day', 'Unlimited', '100 SMS/day', 'JioTV, JioCinema', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'JIO';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Jio 666 Magic', 666.00, 84, '1.5GB/day', 'Unlimited', '100 SMS/day', '5G Unlimited Data', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'JIO';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Jio Data Booster 19', 19.00, 1, '1GB', 'None', 'None', 'Active Plan Required', 'DATA', 1, 0 FROM operators WHERE code = 'JIO';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Jio 2999 Annual', 2999.00, 365, '2.5GB/day', 'Unlimited', '100 SMS/day', 'Prime Video Mobile', 'SPECIAL', 1, 0 FROM operators WHERE code = 'JIO';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Jio ISD Pack 501', 501.00, 28, 'None', 'ISD Talktime', 'None', 'International Calling', 'ISD', 1, 0 FROM operators WHERE code = 'JIO';

-- Seed Plans for Airtel
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Airtel 299 Hero', 299.00, 28, '1.5GB/day', 'Unlimited', '100 SMS/day', 'Wynk Music', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'AIRTEL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Airtel 719 Epic', 719.00, 84, '2.0GB/day', 'Unlimited', '100 SMS/day', 'Disney+ Hotstar 3 Months', 'SPECIAL', 1, 0 FROM operators WHERE code = 'AIRTEL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Airtel Data 58', 58.00, 1, '3GB', 'None', 'None', 'Active Plan Required', 'DATA', 1, 0 FROM operators WHERE code = 'AIRTEL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Airtel 199 Basic', 199.00, 28, '3GB Total', 'Unlimited', '300 SMS', 'Apollo 24|7 Circle', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'AIRTEL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Airtel 3359 Year', 3359.00, 365, '2.5GB/day', 'Unlimited', '100 SMS/day', 'Prime Video Mobile Edition', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'AIRTEL';

-- Seed Plans for VI
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Vi 299 Binge All Night', 299.00, 28, '1.5GB/day', 'Unlimited', '100 SMS/day', 'Binge All Night, Data Rollover', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'VI';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Vi 479 Hero', 479.00, 56, '1.5GB/day', 'Unlimited', '100 SMS/day', 'Binge All Night', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'VI';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Vi 19 Data', 19.00, 1, '1GB', 'None', 'None', 'None', 'DATA', 1, 0 FROM operators WHERE code = 'VI';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'Vi 3099 Annual', 3099.00, 365, '2GB/day', 'Unlimited', '100 SMS/day', 'Disney+ Hotstar 1 Year', 'SPECIAL', 1, 0 FROM operators WHERE code = 'VI';

-- Seed Plans for BSNL
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'BSNL 153 Special', 153.00, 26, '1GB/day', 'Unlimited', '100 SMS/day', 'PRBT', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'BSNL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'BSNL 398 Unlimited Data', 398.00, 30, 'Unlimited', 'Unlimited', '100 SMS/day', 'None', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'BSNL';
INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)
SELECT id, 'BSNL 2973 Yearly', 2973.00, 365, '2GB/day', 'Unlimited', '100 SMS/day', 'None', 'UNLIMITED', 1, 0 FROM operators WHERE code = 'BSNL';
