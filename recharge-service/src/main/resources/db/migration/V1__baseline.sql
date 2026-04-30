-- Baseline Migration for Recharge Service
CREATE TABLE recharges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recharge_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    mobile_number VARCHAR(255) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(255) NOT NULL,
    plan_id BIGINT NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    plan_validity_days INT NOT NULL,
    plan_expiry_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    failure_reason VARCHAR(500),
    transaction_id VARCHAR(255),
    created_by VARCHAR(255),
    created_date DATETIME(6),
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME(6)
);
