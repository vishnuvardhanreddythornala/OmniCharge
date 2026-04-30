-- Baseline Migration for Payment Service
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    recharge_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(500),
    razorpay_order_id VARCHAR(255) UNIQUE,
    razorpay_payment_id VARCHAR(255),
    user_email VARCHAR(255),
    user_mobile VARCHAR(255),
    mobile_number VARCHAR(255),
    operator_name VARCHAR(255),
    plan_name VARCHAR(255),
    created_by VARCHAR(255),
    created_date DATETIME(6),
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME(6)
);
