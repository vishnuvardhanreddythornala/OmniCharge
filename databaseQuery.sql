show databases;
use omnicharge_user_db;
show tables;
select * from refresh_token;
select * from users;



show databases;
use omnicharge_operator_db;
show tables;
select * from operators;
select * from plans;



USE omnicharge_operator_db;
-- ALTER TABLE plans ADD COLUMN deactivated_by_operator BOOLEAN NOT NULL DEFAULT FALSE;

-- UPDATE plans p
-- INNER JOIN operators o ON p.operator_id = o.id
-- SET p.deactivated_by_operator = TRUE
-- WHERE p.is_active = FALSE 
--   AND o.is_active = FALSE;
--   
-- CREATE INDEX idx_plans_deactivated_by_operator ON plans(deactivated_by_operator);
-- CREATE INDEX idx_plans_is_active_operator_id ON plans(is_active, operator_id);



show databases;
use omnicharge_payment_db;
show tables;
select * from transactions;



show databases;
use omnicharge_recharge_db;
show tables;
select * from recharges;


{
    "status": 500,
    "message": "An unexpected error occurred: [403] during [GET] to [http://operator-service/api/plans/5] [OperatorServiceClient#getPlan(Long)]: []",
    "timestamp": "2026-03-22T00:12:28.4010492",
    "path": "/api/recharges"
}

{
    "status": 500,
    "message": "An unexpected error occurred: [403] during [GET] to [http://operator-service/api/plans/2] [OperatorServiceClient#getPlan(Long)]: []",
    "timestamp": "2026-03-22T00:13:29.960691",
    "path": "/api/recharges"
}