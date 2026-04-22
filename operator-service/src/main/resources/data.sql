-- =============================================================
-- Operator & Plan auto-seed (runs on every startup via spring.sql.init)
-- INSERT IGNORE ensures idempotency - safe for restarts
-- =============================================================

INSERT IGNORE INTO operators
  (id, name, code, category, logo_url, is_active, created_date, created_by)
VALUES
  (1,'Reliance Jio','JIO','PREPAID','https://upload.wikimedia.org/wikipedia/commons/4/43/Jio_logo_%282021%29.svg',1,NOW(),'system'),
  (2,'Airtel','AIRTEL','PREPAID','https://upload.wikimedia.org/wikipedia/commons/e/e0/Airtel_logo.svg',1,NOW(),'system'),
  (3,'Vodafone Idea','VI','PREPAID','https://upload.wikimedia.org/wikipedia/commons/2/27/Vi_logo.svg',1,NOW(),'system'),
  (4,'BSNL','BSNL','PREPAID','https://upload.wikimedia.org/wikipedia/commons/c/c5/BSNL_logo.svg',1,NOW(),'system');

INSERT IGNORE INTO plans
  (id, operator_id, plan_name, price, validity_days, data_limit, call_benefit,
   sms_benefit, additional_benefits, category, is_active, deactivated_by_operator,
   created_date, created_by)
VALUES
  (1, 1,'Jio 299 Unlimited',299.00,28,'2GB/day','Unlimited','100 SMS/day','JioTV, JioCinema','UNLIMITED',1,0,NOW(),'system'),
  (2, 1,'Jio 666 Magic',666.00,84,'1.5GB/day','Unlimited','100 SMS/day','5G Unlimited Data','UNLIMITED',1,0,NOW(),'system'),
  (3, 1,'Jio Data Booster 19',19.00,1,'1GB','None','None','Active Plan Required','DATA',1,0,NOW(),'system'),
  (4, 1,'Jio 2999 Annual',2999.00,365,'2.5GB/day','Unlimited','100 SMS/day','Prime Video Mobile','RECOMMENDED',1,0,NOW(),'system'),
  (5, 1,'Jio Talktime 149',149.00,28,'None','149 Talktime','None','ISD Calling','TALKTIME',1,0,NOW(),'system'),
  (6, 2,'Airtel 299 Hero',299.00,28,'1.5GB/day','Unlimited','100 SMS/day','Wynk Music','UNLIMITED',1,0,NOW(),'system'),
  (7, 2,'Airtel 719 Epic',719.00,84,'2.0GB/day','Unlimited','100 SMS/day','Disney+ Hotstar 3M','RECOMMENDED',1,0,NOW(),'system'),
  (8, 2,'Airtel Data 58',58.00,1,'3GB','None','None','Active Plan Required','DATA',1,0,NOW(),'system'),
  (9, 2,'Airtel 199 Basic',199.00,28,'3GB Total','Unlimited','300 SMS','Apollo 24|7 Circle','UNLIMITED',1,0,NOW(),'system'),
  (10,2,'Airtel 3359 Year',3359.00,365,'2.5GB/day','Unlimited','100 SMS/day','Prime Video Mobile','UNLIMITED',1,0,NOW(),'system'),
  (11,3,'Vi 299 Binge',299.00,28,'1.5GB/day','Unlimited','100 SMS/day','Binge All Night','UNLIMITED',1,0,NOW(),'system'),
  (12,3,'Vi 479 Hero',479.00,56,'1.5GB/day','Unlimited','100 SMS/day','Binge All Night','UNLIMITED',1,0,NOW(),'system'),
  (13,3,'Vi 19 Data',19.00,1,'1GB','None','None','None','DATA',1,0,NOW(),'system'),
  (14,3,'Vi 3099 Annual',3099.00,365,'2GB/day','Unlimited','100 SMS/day','Disney+ Hotstar 1Y','RECOMMENDED',1,0,NOW(),'system'),
  (15,4,'BSNL 153 Special',153.00,26,'1GB/day','Unlimited','100 SMS/day','PRBT','UNLIMITED',1,0,NOW(),'system'),
  (16,4,'BSNL 398 Unlimited',398.00,30,'Unlimited','Unlimited','100 SMS/day','None','UNLIMITED',1,0,NOW(),'system'),
  (17,4,'BSNL 2973 Yearly',2973.00,365,'2GB/day','Unlimited','100 SMS/day','None','UNLIMITED',1,0,NOW(),'system');
