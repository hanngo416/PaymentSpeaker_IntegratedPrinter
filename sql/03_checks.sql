-- Check queries. Run AFTER 01_schema.sql and 02_seed.sql
USE PaymentSpeaker;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- CHECK 1: every table has sample data (no table should be empty except Transactions, FraudAlerts and LoginOtps)
SELECT 'Roles' AS table_name, COUNT(*) AS row_count FROM Roles
UNION ALL SELECT 'Users', COUNT(*) FROM Users
UNION ALL SELECT 'LoginOtps', COUNT(*) FROM LoginOtps
UNION ALL SELECT 'Devices', COUNT(*) FROM Devices
UNION ALL SELECT 'Orders', COUNT(*) FROM Orders
UNION ALL SELECT 'Transactions', COUNT(*) FROM Transactions
UNION ALL SELECT 'FraudAlerts', COUNT(*) FROM FraudAlerts
UNION ALL SELECT 'AuditLogs', COUNT(*) FROM AuditLogs;
GO

-- CHECK 2: no password is stored; staff log in by email OTP or Google. Expect 6 accounts
SELECT user_code, email, status FROM Users;
GO

-- CHECK 3: idempotency. The first INSERT succeeds, the second fails with a duplicate key error on bank_trans_id
INSERT INTO Transactions (bank_trans_id, amount, content, status)
VALUES (N'MB_TEST_99999', 50000.00, N'TINKY10234 payment', N'LEGIT');
GO
INSERT INTO Transactions (bank_trans_id, amount, content, status)
VALUES (N'MB_TEST_99999', 50000.00, N'TINKY10234 payment', N'LEGIT');
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
