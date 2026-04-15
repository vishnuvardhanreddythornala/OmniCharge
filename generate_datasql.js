const fs = require('fs');

function generateTransactionAndRechargeSeed() {
    let rechargeSql = `-- Inject 500 mock recharges\n`;
    let paymentSql = `-- Inject 500 mock payments\n`;

    let recharges = [];
    let payments = [];

    const statuses = ['SUCCESS', 'SUCCESS', 'SUCCESS', 'FAILED', 'PENDING'];
    for(let i=1; i<=200; i++) {
        const id = `OMNI-MOCK-${i}`;
        const txnId = `TXN-MOCK-${i}`;
        const status = statuses[i % 5];
        
        recharges.push(`INSERT INTO recharges (recharge_id, user_id, mobile_number, operator_id, operator_name, plan_id, plan_name, amount, plan_validity_days, plan_expiry_date, status, transaction_id, created_at, updated_at) VALUES ('${id}', ${i}, '+9198000${String(i).padStart(5, '0')}', 1, 'Fake Operator', 1, 'Fake Plan', 299.00, 28, DATE_ADD(CURDATE(), INTERVAL 28 DAY), '${status}', '${txnId}', DATE_SUB(NOW(), INTERVAL ${i} HOUR), NOW());`);
        
        payments.push(`INSERT INTO transactions (transaction_id, user_id, requested_by_email, requested_by_mobile, amount, recharge_id, payment_method, status, created_at, updated_at) VALUES ('${txnId}', ${i}, 'bulkuser${i}@omnicharge.com', '+9198000${String(i).padStart(5, '0')}', 299.00, '${id}', 'RAZORPAY', '${status}', DATE_SUB(NOW(), INTERVAL ${i} HOUR), NOW());`);
    }

    fs.writeFileSync('./recharge-service/src/main/resources/data.sql', rechargeSql + recharges.join('\n') + '\n');
    console.log('Generated data.sql for recharge-service');

    fs.writeFileSync('./payment-service/src/main/resources/data.sql', paymentSql + payments.join('\n') + '\n');
    console.log('Generated data.sql for payment-service');
}

generateTransactionAndRechargeSeed();
