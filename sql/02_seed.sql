-- Sample data. Run AFTER 01_schema.sql
USE PaymentSpeaker;
GO
-- Clear old data before loading again
DELETE FROM AuditLogs;
DELETE FROM FraudAlerts;
DELETE FROM Transactions;
DELETE FROM Orders;
DELETE FROM MaintenanceTickets;
DELETE FROM Devices;
DELETE FROM LoginOtps;
DELETE FROM Users;
DELETE FROM Roles;
GO

-- =====================================================================================
-- 4. SAMPLE DATA
-- =====================================================================================

-- 4.1. Roles
INSERT INTO Roles (role_id, role_name, description) VALUES
(1, N'ADMIN', N'System administrator: full configuration and staff management rights'),
(2, N'STORE_MANAGER', N'Store manager: manages revenue and cashier accounts'),
(3, N'CASHIER', N'Counter cashier: creates orders, hears the speaker and prints receipts'),
(4, N'AUDITOR', N'Auditor: investigates fraudulent transactions and checks data integrity'),
(5, N'TECHNICIAN', N'Technician: maintains the speaker and printer hardware');
GO

-- 4.2. Users
-- No password: staff log in by email OTP or Google.
INSERT INTO Users (user_code, full_name, email, role_id, status) VALUES
(N'han_admin', N'Ngô Ngọc Gia Hân', N'hanngo816@gmail.com', 1, N'ACTIVE'),
(N'mai_manager', N'Đỗ Thị Mai', N'hanngo816+manager@gmail.com', 2, N'ACTIVE'),
(N'nam_cashier', N'Bùi Văn Nam', N'hanngo816+cashier@gmail.com', 3, N'ACTIVE'),
(N'dung_auditor', N'Phạm Quốc Dũng', N'hanngo816+auditor@gmail.com', 4, N'ACTIVE'),
(N'em_technician', N'Hoàng Văn Em', N'hanngo816+tech@gmail.com', 5, N'ACTIVE'),
(N'ha_cashier', N'Vũ Thị Hà', N'hanngo816+locked@gmail.com', 3, N'LOCKED');
GO

-- 4.3. Devices
INSERT INTO Devices (device_code, device_type, model_name, api_key, status) VALUES
(N'SPK_01', N'SPEAKER', N'Smart Voice Box Pro V2', N'KEY_SPK_01_SECRET_9876', N'ONLINE'),
(N'PRN_01', N'PRINTER', N'Aiyin A70 Thermal 80mm', N'KEY_PRN_01_SECRET_5432', N'ONLINE');
GO

-- 4.4. Sample orders
-- Order 1: a valid pending payment
INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at) VALUES
(N'TINKY10234', 3, N'nam_cashier', 50000.00, N'PENDING', N'2|99|0987654321|NGUYEN VAN A||0|0|50000|TINKY10234|', DATEADD(MINUTE, 15, SYSDATETIME())),
-- Order 2: pending payment used to test an underpaid transfer
(N'TINKY10235', 3, N'nam_cashier', 120000.00, N'PENDING', N'2|99|0987654321|NGUYEN VAN A||0|0|120000|TINKY10235|', DATEADD(MINUTE, 15, SYSDATETIME()));
GO

-- 4.5. Genesis block of AuditLogs (first block of the hash chain)
-- The first block always has a prev_hash of 64 '0' characters
INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, prev_hash, curr_hash, created_at) VALUES
(N'SYSTEM', N'INIT_SYSTEM', N'SYSTEM', N'0', N'{"message": "Genesis block created. System initialized."}',
 '0000000000000000000000000000000000000000000000000000000000000000',
 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
 SYSDATETIME());
GO

PRINT N'>>> Database and sample data for PaymentSpeaker loaded successfully!';
GO
