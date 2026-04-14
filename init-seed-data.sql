-- ============================================================
-- OmniCharge Production-Grade Seed Data
-- Run AFTER all services have started and tables are created
-- This seeds: recharges, transactions, notifications
-- Target: 1000+ total rows across system
-- ============================================================

-- ============================================================
-- RECHARGES: 300 rows across various users/operators/plans
-- ============================================================
USE omnicharge_recharge_db;

-- Helper: Use a procedure to generate bulk recharges programmatically
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS seed_recharges()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_user_id INT;
    DECLARE v_operator_id INT;
    DECLARE v_plan_id INT;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_status VARCHAR(20);
    DECLARE v_operator_name VARCHAR(100);
    DECLARE v_plan_name VARCHAR(100);
    DECLARE v_validity INT;
    DECLARE v_days_ago INT;
    DECLARE v_mobile VARCHAR(20);
    
    WHILE i <= 300 DO
        SET v_user_id = (i MOD 200) + 1;
        SET v_operator_id = (i MOD 4) + 1;
        SET v_days_ago = FLOOR(RAND() * 90);
        
        -- Rotate through realistic amounts / plan names
        CASE v_operator_id
            WHEN 1 THEN 
                SET v_operator_name = 'Reliance Jio';
                CASE (i MOD 5)
                    WHEN 0 THEN SET v_plan_name = 'Jio 299 Unlimited'; SET v_amount = 299.00; SET v_validity = 28;
                    WHEN 1 THEN SET v_plan_name = 'Jio 666 Magic'; SET v_amount = 666.00; SET v_validity = 84;
                    WHEN 2 THEN SET v_plan_name = 'Jio Data Booster 19'; SET v_amount = 19.00; SET v_validity = 1;
                    WHEN 3 THEN SET v_plan_name = 'Jio 2999 Annual'; SET v_amount = 2999.00; SET v_validity = 365;
                    ELSE SET v_plan_name = 'Jio ISD Pack 501'; SET v_amount = 501.00; SET v_validity = 28;
                END CASE;
            WHEN 2 THEN
                SET v_operator_name = 'Airtel';
                CASE (i MOD 5)
                    WHEN 0 THEN SET v_plan_name = 'Airtel 299 Hero'; SET v_amount = 299.00; SET v_validity = 28;
                    WHEN 1 THEN SET v_plan_name = 'Airtel 719 Epic'; SET v_amount = 719.00; SET v_validity = 84;
                    WHEN 2 THEN SET v_plan_name = 'Airtel Data 58'; SET v_amount = 58.00; SET v_validity = 1;
                    WHEN 3 THEN SET v_plan_name = 'Airtel 199 Basic'; SET v_amount = 199.00; SET v_validity = 28;
                    ELSE SET v_plan_name = 'Airtel 3359 Year'; SET v_amount = 3359.00; SET v_validity = 365;
                END CASE;
            WHEN 3 THEN
                SET v_operator_name = 'Vodafone Idea';
                CASE (i MOD 4)
                    WHEN 0 THEN SET v_plan_name = 'Vi 299 Binge All Night'; SET v_amount = 299.00; SET v_validity = 28;
                    WHEN 1 THEN SET v_plan_name = 'Vi 479 Hero'; SET v_amount = 479.00; SET v_validity = 56;
                    WHEN 2 THEN SET v_plan_name = 'Vi 19 Data'; SET v_amount = 19.00; SET v_validity = 1;
                    ELSE SET v_plan_name = 'Vi 3099 Annual'; SET v_amount = 3099.00; SET v_validity = 365;
                END CASE;
            ELSE
                SET v_operator_name = 'BSNL';
                CASE (i MOD 3)
                    WHEN 0 THEN SET v_plan_name = 'BSNL 153 Special'; SET v_amount = 153.00; SET v_validity = 26;
                    WHEN 1 THEN SET v_plan_name = 'BSNL 398 Unlimited Data'; SET v_amount = 398.00; SET v_validity = 30;
                    ELSE SET v_plan_name = 'BSNL 2973 Yearly'; SET v_amount = 2973.00; SET v_validity = 365;
                END CASE;
        END CASE;
        
        -- Determine status distribution: 75% SUCCESS, 10% FAILED, 10% EXPIRED, 5% PROCESSING
        IF i MOD 20 = 0 THEN SET v_status = 'PROCESSING';
        ELSEIF i MOD 10 IN (1, 3) THEN SET v_status = 'FAILED';
        ELSEIF i MOD 10 IN (5, 7) THEN SET v_status = 'EXPIRED';
        ELSE SET v_status = 'SUCCESS';
        END IF;
        
        SET v_mobile = CONCAT('+91980000', LPAD(v_user_id, 4, '0'));
        SET v_plan_id = v_operator_id * 3 + (i MOD 3);
        
        INSERT IGNORE INTO recharges (
            recharge_id, user_id, mobile_number, operator_id, operator_name,
            plan_id, plan_name, amount, plan_validity_days, plan_expiry_date,
            status, failure_reason, transaction_id,
            created_date, last_modified_date
        ) VALUES (
            CONCAT('RCH-', LPAD(i, 6, '0')),
            v_user_id,
            v_mobile,
            v_operator_id,
            v_operator_name,
            v_plan_id,
            v_plan_name,
            v_amount,
            v_validity,
            DATE_ADD(DATE_SUB(CURDATE(), INTERVAL v_days_ago DAY), INTERVAL v_validity DAY),
            v_status,
            CASE WHEN v_status = 'FAILED' THEN 'Payment gateway timeout' ELSE NULL END,
            CASE WHEN v_status IN ('SUCCESS', 'EXPIRED') THEN CONCAT('TXN-', LPAD(i, 6, '0')) ELSE NULL END,
            DATE_SUB(NOW(), INTERVAL v_days_ago DAY),
            DATE_SUB(NOW(), INTERVAL v_days_ago DAY)
        );
        
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_recharges();
DROP PROCEDURE IF EXISTS seed_recharges;

-- ============================================================
-- TRANSACTIONS: 300 rows matching successful recharges
-- ============================================================
USE omnicharge_payment_db;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS seed_transactions()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_user_id INT;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_status VARCHAR(20);
    DECLARE v_method VARCHAR(20);
    DECLARE v_days_ago INT;
    
    WHILE i <= 300 DO
        SET v_user_id = (i MOD 200) + 1;
        SET v_days_ago = FLOOR(RAND() * 90);
        
        -- Randomize amounts
        CASE (i MOD 8)
            WHEN 0 THEN SET v_amount = 299.00;
            WHEN 1 THEN SET v_amount = 666.00;
            WHEN 2 THEN SET v_amount = 19.00;
            WHEN 3 THEN SET v_amount = 199.00;
            WHEN 4 THEN SET v_amount = 479.00;
            WHEN 5 THEN SET v_amount = 153.00;
            WHEN 6 THEN SET v_amount = 719.00;
            ELSE SET v_amount = 398.00;
        END CASE;
        
        -- Payment method rotation
        CASE (i MOD 5)
            WHEN 0 THEN SET v_method = 'UPI';
            WHEN 1 THEN SET v_method = 'CREDIT_CARD';
            WHEN 2 THEN SET v_method = 'DEBIT_CARD';
            WHEN 3 THEN SET v_method = 'NET_BANKING';
            ELSE SET v_method = 'RAZORPAY';
        END CASE;
        
        -- Status distribution: 80% SUCCESS, 12% FAILED, 8% PENDING
        IF i MOD 25 IN (0, 5) THEN SET v_status = 'PENDING';
        ELSEIF i MOD 8 IN (3, 7) THEN SET v_status = 'FAILED';
        ELSE SET v_status = 'SUCCESS';
        END IF;
        
        INSERT IGNORE INTO transactions (
            transaction_id, recharge_id, user_id, amount,
            payment_method, status, failure_reason,
            razorpay_order_id, razorpay_payment_id,
            user_email, user_mobile, mobile_number, operator_name, plan_name,
            created_date, last_modified_date
        ) VALUES (
            CONCAT('TXN-', LPAD(i, 6, '0')),
            CONCAT('RCH-', LPAD(i, 6, '0')),
            v_user_id,
            v_amount,
            v_method,
            v_status,
            CASE WHEN v_status = 'FAILED' THEN 'Insufficient funds' ELSE NULL END,
            CONCAT('order_', SUBSTRING(MD5(CONCAT('order', i)), 1, 14)),
            CASE WHEN v_status = 'SUCCESS' THEN CONCAT('pay_', SUBSTRING(MD5(CONCAT('pay', i)), 1, 14)) ELSE NULL END,
            CONCAT('user', v_user_id, '@omnicharge.com'),
            CONCAT('+91980000', LPAD(v_user_id, 4, '0')),
            CONCAT('+91980000', LPAD(v_user_id, 4, '0')),
            CASE (i MOD 4) WHEN 0 THEN 'Reliance Jio' WHEN 1 THEN 'Airtel' WHEN 2 THEN 'Vodafone Idea' ELSE 'BSNL' END,
            CASE (i MOD 4) WHEN 0 THEN 'Jio 299 Unlimited' WHEN 1 THEN 'Airtel 299 Hero' WHEN 2 THEN 'Vi 299 Binge All Night' ELSE 'BSNL 153 Special' END,
            DATE_SUB(NOW(), INTERVAL v_days_ago DAY),
            DATE_SUB(NOW(), INTERVAL v_days_ago DAY)
        );
        
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_transactions();
DROP PROCEDURE IF EXISTS seed_transactions;

-- ============================================================
-- NOTIFICATIONS: 400 rows across users
-- ============================================================
USE omnicharge_notification_db;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS seed_notifications()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_user_id INT;
    DECLARE v_type VARCHAR(20);
    DECLARE v_category VARCHAR(30);
    DECLARE v_subject VARCHAR(255);
    DECLARE v_message VARCHAR(2000);
    DECLARE v_status VARCHAR(20);
    DECLARE v_days_ago INT;
    
    WHILE i <= 400 DO
        SET v_user_id = (i MOD 200) + 1;
        SET v_days_ago = FLOOR(RAND() * 90);
        
        -- Type rotation
        IF i MOD 2 = 0 THEN SET v_type = 'EMAIL'; ELSE SET v_type = 'SMS'; END IF;
        
        -- Category / content rotation
        CASE (i MOD 4)
            WHEN 0 THEN 
                SET v_category = 'PAYMENT_SUCCESS';
                SET v_subject = 'Payment Successful';
                SET v_message = CONCAT('Your payment of ₹', (i MOD 5 + 1) * 100, ' has been processed successfully. Transaction ID: TXN-', LPAD(i, 6, '0'));
            WHEN 1 THEN 
                SET v_category = 'PAYMENT_FAILED';
                SET v_subject = 'Payment Failed';
                SET v_message = CONCAT('Your payment of ₹', (i MOD 3 + 1) * 150, ' could not be processed. Please try again or contact support.');
            WHEN 2 THEN 
                SET v_category = 'PLAN_EXPIRY_REMINDER';
                SET v_subject = 'Plan Expiring Soon';
                SET v_message = CONCAT('Your current plan will expire in ', (i MOD 7 + 1), ' days. Recharge now to continue enjoying uninterrupted services.');
            ELSE 
                SET v_category = 'PLAN_EXPIRED';
                SET v_subject = 'Plan Expired';
                SET v_message = 'Your recharge plan has expired. Recharge now to restore your services.';
        END CASE;
        
        -- Status: 90% SENT, 5% FAILED, 5% PENDING
        IF i MOD 20 = 0 THEN SET v_status = 'PENDING';
        ELSEIF i MOD 20 = 10 THEN SET v_status = 'FAILED';
        ELSE SET v_status = 'SENT';
        END IF;
        
        INSERT IGNORE INTO notifications (
            user_id, user_email, user_mobile, type, category,
            subject, message, status, reference_id, is_read,
            created_date, last_modified_date
        ) VALUES (
            v_user_id,
            CONCAT('user', v_user_id, '@omnicharge.com'),
            CONCAT('+91980000', LPAD(v_user_id, 4, '0')),
            v_type,
            v_category,
            v_subject,
            v_message,
            v_status,
            CONCAT('REF-', LPAD(i, 6, '0')),
            CASE WHEN i MOD 3 = 0 THEN 1 ELSE 0 END,
            DATE_SUB(NOW(), INTERVAL v_days_ago DAY),
            DATE_SUB(NOW(), INTERVAL v_days_ago DAY)
        );
        
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_notifications();
DROP PROCEDURE IF EXISTS seed_notifications;
