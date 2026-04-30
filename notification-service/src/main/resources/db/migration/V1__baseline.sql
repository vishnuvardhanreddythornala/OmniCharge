-- Baseline Migration for Notification Service
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_mobile VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(255) NOT NULL,
    reference_id VARCHAR(255),
    is_read BOOLEAN NOT NULL,
    created_by VARCHAR(255),
    created_date DATETIME(6),
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME(6)
);

CREATE TABLE notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255) NOT NULL UNIQUE,
    email_subject VARCHAR(500) NOT NULL,
    email_body TEXT NOT NULL,
    sms_body VARCHAR(1000) NOT NULL,
    is_active BOOLEAN NOT NULL,
    description VARCHAR(1000),
    created_by VARCHAR(255),
    created_date DATETIME(6),
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME(6)
);

CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(255) NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    sms_enabled BOOLEAN NOT NULL,
    is_enabled BOOLEAN NOT NULL,
    created_by VARCHAR(255),
    created_date DATETIME(6),
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME(6),
    UNIQUE (user_id, category)
);
