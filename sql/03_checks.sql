-- Check queries. Run AFTER 01_schema.sql and 02_seed.sql
USE PaymentSpeaker;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- CHECK 1: row count of every table after a clean 01 + 02 run. Compare row_count with expected
SELECT 'Roles' AS table_name, COUNT(*) AS row_count, 5 AS expected FROM Roles
UNION ALL SELECT 'Users', COUNT(*), 6 FROM Users
UNION ALL SELECT 'LoginOtps', COUNT(*), 0 FROM LoginOtps
UNION ALL SELECT 'Devices', COUNT(*), 1 FROM Devices
UNION ALL SELECT 'MaintenanceTickets', COUNT(*), 1 FROM MaintenanceTickets
UNION ALL SELECT 'Orders', COUNT(*), 39 FROM Orders
UNION ALL SELECT 'Transactions', COUNT(*), 40 FROM Transactions
UNION ALL SELECT 'FraudAlerts', COUNT(*), 8 FROM FraudAlerts
UNION ALL SELECT 'AuditLogs', COUNT(*), 14 FROM AuditLogs;
GO

-- CHECK 1b: the eight foreign keys, child table first
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

-- CHECK 5: the genesis block of the audit chain exists (prev_hash is 64 zeros)
SELECT log_id, actor_username, action, prev_hash, curr_hash
FROM AuditLogs;
GO
