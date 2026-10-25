-- Check queries. Run AFTER 01_schema.sql and 02_seed.sql
USE PaymentSpeaker;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- CHECK 1: row count of every table after a clean 01 + 02 run. Compare row_count with expected
SELECT 'Roles' AS table_name, COUNT(*) AS row_count, 6 AS expected FROM Roles
UNION ALL SELECT 'Users', COUNT(*), 7 FROM Users
UNION ALL SELECT 'LoginOtps', COUNT(*), 0 FROM LoginOtps
UNION ALL SELECT 'Devices', COUNT(*), 1 FROM Devices
UNION ALL SELECT 'Categories', COUNT(*), 5 FROM Categories
UNION ALL SELECT 'Groups', COUNT(*), 15 FROM [Groups]
UNION ALL SELECT 'Products', COUNT(*), 51 FROM Products
UNION ALL SELECT 'Orders', COUNT(*), 39 FROM Orders
UNION ALL SELECT 'OrderItems', COUNT(*), 7 FROM OrderItems
UNION ALL SELECT 'Transactions', COUNT(*), 40 FROM Transactions
UNION ALL SELECT 'FraudAlerts', COUNT(*), 8 FROM FraudAlerts
UNION ALL SELECT 'AuditLogs', COUNT(*), 14 FROM AuditLogs
UNION ALL SELECT 'DiscountCodes', COUNT(*), 3 FROM DiscountCodes
UNION ALL SELECT 'Settings', COUNT(*), 6 FROM Settings
UNION ALL SELECT 'TaxRules', COUNT(*), 14 FROM TaxRules
UNION ALL SELECT 'Affiliates', COUNT(*), 5 FROM Affiliates
UNION ALL SELECT 'Campaigns', COUNT(*), 3 FROM Campaigns
UNION ALL SELECT 'Commissions', COUNT(*), 15 FROM Commissions
UNION ALL SELECT 'Payouts', COUNT(*), 1 FROM Payouts
UNION ALL SELECT 'CommissionAdjustments', COUNT(*), 0 FROM CommissionAdjustments
UNION ALL SELECT 'TaxObligations', COUNT(*), 5 FROM TaxObligations
UNION ALL SELECT 'TaxObligationHistory', COUNT(*), 1 FROM TaxObligationHistory
UNION ALL SELECT 'TaxPayments', COUNT(*), 1 FROM TaxPayments
UNION ALL SELECT 'TaxCalculations', COUNT(*), 0 FROM TaxCalculations
UNION ALL SELECT 'TaxAssessments', COUNT(*), 0 FROM TaxAssessments
UNION ALL SELECT 'Expenses', COUNT(*), 8 FROM Expenses
UNION ALL SELECT 'TaxReconNotes', COUNT(*), 0 FROM TaxReconNotes
UNION ALL SELECT 'TaxDocuments', COUNT(*), 4 FROM TaxDocuments
UNION ALL SELECT 'ChannelSales', COUNT(*), 56 FROM ChannelSales
UNION ALL SELECT 'ChannelCommissions', COUNT(*), 6 FROM ChannelCommissions
UNION ALL SELECT 'ChannelPayouts', COUNT(*), 1 FROM ChannelPayouts
UNION ALL SELECT 'ChannelPayoutItems', COUNT(*), 1 FROM ChannelPayoutItems;
GO

-- CHECK 1b: every foreign key in the database, child table first
SELECT fk.name AS foreign_key, OBJECT_NAME(fk.parent_object_id) AS child_table, OBJECT_NAME(fk.referenced_object_id) AS parent_table
FROM sys.foreign_keys fk ORDER BY child_table, foreign_key;
GO

-- CHECK 1c: the time columns are real date-time types
SELECT COUNT(*) AS transactions, MIN(trans_time) AS first_time, MAX(trans_time) AS last_time FROM Transactions;
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Transactions' AND COLUMN_NAME IN ('trans_time', 'received_at');
GO

-- CHECK 2: no password is stored; staff log in by email OTP or Google. Expect 6 accounts
SELECT user_code, email, status FROM Users;
GO

-- CHECK 3: idempotency. The first INSERT succeeds, the second fails with a duplicate key error on bank_trans_id
INSERT INTO Transactions (bank_trans_id, amount, content, status)
VALUES (N'MB_TEST_99999', 50000.00, N'TINKY10234 payment', N'COMPLETED');
GO
INSERT INTO Transactions (bank_trans_id, amount, content, status)
VALUES (N'MB_TEST_99999', 50000.00, N'TINKY10234 payment', N'COMPLETED');
GO
DELETE FROM Transactions WHERE bank_trans_id = N'MB_TEST_99999';
GO

-- CHECK 4: a user that still has orders cannot be deleted (expect a foreign key error)
DECLARE @cashier_uid INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');
DELETE FROM Users WHERE user_id = @cashier_uid;
GO

-- CHECK 5: the audit log holds the first record and the shift history
SELECT log_id, actor_username, action, created_at
FROM AuditLogs;
GO

-- CHECK 6: every tax rule (kind, sector) has exactly one active row in force today (WARN_LEVEL has three). Expect no rows
SELECT kind, sector, COUNT(*) AS rows_in_force
FROM TaxRules
WHERE active = 1 AND effective_from <= CAST(SYSDATETIME() AS DATE)
  AND (effective_to IS NULL OR effective_to >= CAST(SYSDATETIME() AS DATE))
GROUP BY kind, sector
HAVING COUNT(*) <> CASE WHEN kind = 'WARN_LEVEL' THEN 3 ELSE 1 END;
GO

-- CHECK 7: no product has a negative cost price. Expect no rows
SELECT product_id, product_code, cost_price FROM Products WHERE cost_price < 0;
GO

-- CHECK 8: no duplicate (channel, external_ref) in ChannelSales. Expect no rows
SELECT channel, external_ref, COUNT(*) AS copies
FROM ChannelSales
GROUP BY channel, external_ref
HAVING COUNT(*) > 1;
GO

-- CHECK: every commission belongs to a referred order and its affiliate matches the order. Expect no rows
SELECT c.commission_id, c.order_id FROM Commissions c
JOIN Orders o ON o.order_id = c.order_id
WHERE o.referred_by_affiliate_id IS NULL OR o.referred_by_affiliate_id <> c.affiliate_id;
GO

-- CHECK: a tax obligation marked PAID must be covered by payments with evidence. Expect no rows
SELECT o.obligation_id FROM TaxObligations o
WHERE o.status = 'PAID' AND (o.confirmed_amount IS NULL OR o.confirmed_amount > ISNULL((SELECT SUM(amount) FROM TaxPayments p WHERE p.obligation_id = o.obligation_id), 0));
GO
