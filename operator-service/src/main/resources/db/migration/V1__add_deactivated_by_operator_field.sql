-- Migration script to add deactivatedByOperator field to plans table
-- This field tracks whether a plan was deactivated due to operator deactivation

-- Add new column to plans table
ALTER TABLE plans 
ADD COLUMN deactivated_by_operator BOOLEAN NOT NULL DEFAULT FALSE;

-- Set deactivatedByOperator = TRUE for inactive plans of inactive operators
-- These plans were likely deactivated when their operator was deactivated
UPDATE plans p
INNER JOIN operators o ON p.operator_id = o.id
SET p.deactivated_by_operator = TRUE
WHERE p.is_active = FALSE 
  AND o.is_active = FALSE;

-- Add index for better query performance
CREATE INDEX idx_plans_deactivated_by_operator ON plans(deactivated_by_operator);
CREATE INDEX idx_plans_is_active_operator_id ON plans(is_active, operator_id);
