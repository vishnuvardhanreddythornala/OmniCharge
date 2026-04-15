const fs = require('fs');

function generateUserBulk() {
    let sql = `-- V3: Bulk seed remaining dummy users to reach 1000\n`;
    sql += `INSERT IGNORE INTO users (email, full_name, mobile_number, password, auth_provider, role, is_active, is_mobile_verified, is_email_verified) VALUES\n`;
    
    // V2 had 200 users. We will generate 800 more.
    let values = [];
    // using a dummy bcrypt hash
    const fakePass = '$2a$10$xyz'; 
    for(let i=201; i<=1000; i++) {
        const number = `+9198000${String(i).padStart(5, '0')}`;
        const email = `bulkuser${i}@omnicharge.com`;
        values.push(`('${email}','Bulk User ${i}','${number}','${fakePass}','LOCAL','ROLE_USER',1,1,1)`);
    }
    sql += values.join(',\n') + ';\n';
    
    fs.writeFileSync('./user-service/src/main/resources/db/migration/V3__seed_bulk_users.sql', sql);
    console.log('Generated V3__seed_bulk_users.sql');
}

function generateOperatorBulk() {
    let sql = `-- V3: Bulk seed 50 operators and 500 plans\n\n`;
    sql += `-- Seed 46 dummy operators (4 exist in V2)\n`;
    sql += `INSERT IGNORE INTO operators (name, code, category, logo_url, is_active) VALUES\n`;
    
    let opValues = [];
    for(let i=5; i<=50; i++) {
        opValues.push(`('Mock Operator ${i}', 'MOCK${i}', 'PREPAID', null, 1)`);
    }
    sql += opValues.join(',\n') + ';\n\n';

    sql += `-- Seed 10 plans per dummy operator\n`;
    sql += `INSERT INTO plans (operator_id, plan_name, price, validity_days, data_limit, call_benefit, sms_benefit, additional_benefits, category, is_active, deactivated_by_operator)\n`;
    sql += `SELECT id, CONCAT('Bulk Plan ', code, '-', numbers.n), 100 + numbers.n * 10, 28, '1GB/day', 'Unlimited', '100 SMS/day', NULL, 'DATA', 1, 0\n`;
    sql += `FROM operators\n`;
    sql += `JOIN (SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) numbers\n`;
    sql += `WHERE code LIKE 'MOCK%';\n`;

    fs.writeFileSync('./operator-service/src/main/resources/db/migration/V3__seed_bulk_operators_plans.sql', sql);
    console.log('Generated V3__seed_bulk_operators_plans.sql');
}

function generateTransactionBulkForReporting() {
    // We cannot easily inject into transaction and recharge services because they dont have Flyway!
    console.log('Payment & Recharge services do not use Flyway. Skipping.');
}

generateUserBulk();
generateOperatorBulk();
generateTransactionBulkForReporting();
