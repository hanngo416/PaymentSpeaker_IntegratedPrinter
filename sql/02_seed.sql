-- Sample data. Run AFTER 01_schema.sql
USE PaymentSpeaker;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
-- Safety guard: seed only an EMPTY database, never overwrite existing (real) data.
-- To start over, run 99_reset_demo.sql and 01_schema.sql first.
IF EXISTS (SELECT 1 FROM Users) OR EXISTS (SELECT 1 FROM Transactions)
BEGIN
    PRINT N'>>> Database already has data - seed skipped. Run 99_reset_demo.sql first for a reset.';
    SET NOEXEC ON;
END
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
(N'han_admin', N'Ngo Ngoc Gia Han', N'hanngo816@gmail.com', 1, N'ACTIVE'),
(N'mai_manager', N'Do Thi Mai', N'hanngo816+manager@gmail.com', 2, N'ACTIVE'),
(N'nam_cashier', N'Bui Van Nam', N'hanngo816+cashier@gmail.com', 3, N'ACTIVE'),
(N'dung_auditor', N'Pham Quoc Dung', N'hanngo816+auditor@gmail.com', 4, N'ACTIVE'),
(N'em_technician', N'Hoang Van Em', N'hanngo816+tech@gmail.com', 5, N'ACTIVE'),
(N'ha_cashier', N'Vu Thi Ha', N'hanngo816+locked@gmail.com', 3, N'LOCKED');
GO

-- 4.3. Devices
INSERT INTO Devices (device_code, device_type, model_name, api_key, status) VALUES
(N'SPK_01', N'SPEAKER', N'Smart Voice Box Pro V2', N'KEY_SPK_01_SECRET_9876', N'ONLINE'),
(N'PRN_01', N'PRINTER', N'Aiyin A70 Thermal 80mm', N'KEY_PRN_01_SECRET_5432', N'ONLINE');
GO

-- 4.4. Sample orders
-- Order 1: a valid pending payment
DECLARE @cashier_id INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');

INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at) VALUES
(N'TINKY10234', @cashier_id, N'nam_cashier', 50000.00, N'PENDING',
 N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=50000&addInfo=TINKY10234&accountName=LAM%20HOANG%20AN',
 DATEADD(MINUTE, 60, SYSDATETIME())),
-- Order 2: pending payment used to test an underpaid transfer
(N'TINKY10235', @cashier_id, N'nam_cashier', 120000.00, N'PENDING',
 N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=120000&addInfo=TINKY10235&accountName=LAM%20HOANG%20AN',
 DATEADD(MINUTE, 60, SYSDATETIME()));
GO

-- 4.5. Genesis block of AuditLogs (first block of the hash chain)
-- The first block always has a prev_hash of 64 '0' characters
INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, prev_hash, curr_hash, created_at) VALUES
(N'SYSTEM', N'INIT_SYSTEM', N'SYSTEM', N'0', N'{"message": "Genesis block created. System initialized."}',
 '0000000000000000000000000000000000000000000000000000000000000000',
 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
 SYSDATETIME());
GO

-- 4.6. Demo data: transfers, their orders and the alerts the rules engine would raise
-- 32 transfers over the last 7 days (today included), 8 alerts (6 NEW, 1 RESOLVED, 1 DISMISSED).
DECLARE @demo TABLE (
    n INT, days_ago INT, hh INT, mm INT, ss INT, amount DECIMAL(18,2), kind NVARCHAR(30),
    order_amount DECIMAL(18,2), cname NVARCHAR(60), content NVARCHAR(200)
);

-- kind: LEGIT, NO_ORDER (no TINKY code in the note), DUP_TRANS, AMOUNT_MISMATCH, HIGH_AMOUNT, BURST_RATE
-- content NULL = "<customer name> chuyen tien <order code>", like a real bank note.
INSERT INTO @demo (n, days_ago, hh, mm, ss, amount, kind, order_amount, cname, content) VALUES
(1,  6,  8, 12, 41,   45000, N'LEGIT',           45000, N'NGUYEN VAN BINH',   NULL),
(2,  6, 10, 37, 18,  128000, N'LEGIT',          128000, N'TRAN THI LAN',      NULL),
(3,  6, 15,  5, 52,   69000, N'LEGIT',           69000, N'LE MINH KHOA',      NULL),
(4,  5,  9, 23,  7,   89000, N'LEGIT',           89000, N'PHAM THU HA',       NULL),
(5,  5, 12, 48, 33,  215000, N'LEGIT',          215000, N'VO HOANG LONG',     NULL),
(6,  5, 16, 14, 26,   38000, N'LEGIT',           38000, N'DANG QUOC VIET',    NULL),
(7,  5, 17, 31, 59,  150000, N'NO_ORDER',          NULL, N'BUI THANH TUNG',   N'BUI THANH TUNG chuyen tien'),
(8,  4, 10,  2, 14,   97000, N'LEGIT',           97000, N'HOANG THI MY',      NULL),
(9,  4, 12, 19, 45,  312000, N'LEGIT',          312000, N'NGO DUC ANH',       NULL),
(10, 4, 14, 41,  3,   76000, N'LEGIT',           76000, N'DO KIM NGAN',       NULL),
(11, 4, 15, 56, 38,   75000, N'AMOUNT_MISMATCH', 80000, N'LY GIA BAO',        NULL),
(12, 3,  9, 16, 20,  118000, N'LEGIT',          118000, N'TRUONG MY LINH',    NULL),
(13, 3, 11, 44, 51,   62000, N'LEGIT',           62000, N'PHAN VAN DUNG',     NULL),
(14, 3, 12, 33,  9,   60000, N'NO_ORDER',          NULL, N'CAO THI THAO',     N'CAO THI THAO ca phe'),
(15, 3, 12, 33, 40,   60000, N'DUP_TRANS',         NULL, N'CAO THI THAO',     N'CAO THI THAO ca phe'),
(16, 3, 16, 27, 12, 6500000, N'HIGH_AMOUNT',  6500000, N'CONG TY TNHH MINH PHAT', NULL),
(17, 2,  8, 51, 29,   72000, N'LEGIT',           72000, N'VU THI HONG',       NULL),
(18, 2, 10, 24,  6,  135000, N'LEGIT',          135000, N'DINH VAN SON',      NULL),
(19, 2, 14, 12, 47,   94000, N'LEGIT',           94000, N'LUONG BICH TRAM',   NULL),
(20, 2, 16, 39, 15,   55000, N'LEGIT',           55000, N'NGUYEN HUU PHUOC',  NULL),
(21, 1,  9,  8, 34,   85000, N'LEGIT',           85000, N'TA QUYNH NHU',      NULL),
(22, 1, 11, 52, 22,  146000, N'LEGIT',          146000, N'TRINH CONG HAU',    NULL),
(23, 1, 13, 17, 58,   58000, N'LEGIT',           58000, N'MAI PHUONG ANH',    NULL),
(24, 1, 15, 46,  1, 7200000, N'HIGH_AMOUNT',  7200000, N'CONG TY CP AN KHANG', NULL),
(25, 1, 18,  9, 36,   50000, N'AMOUNT_MISMATCH', 45000, N'HUYNH NGOC TRAN',   NULL),
(26, 0,  8, 35, 11,  105000, N'LEGIT',          105000, N'LAM GIA KIET',      NULL),
(27, 0,  9, 20, 48,  122000, N'LEGIT',          122000, N'TO THI BICH',       NULL),
-- Rush hour: 5 transfers inside one minute, the 5th one triggers BURST_RATE
(28, 0, 10, 41,  4,   48000, N'LEGIT',           48000, N'VAN THANH DAT',     NULL),
(29, 0, 10, 41, 15,   65000, N'LEGIT',           65000, N'KIEU MY DUYEN',     NULL),
(30, 0, 10, 41, 27,   39000, N'LEGIT',           39000, N'DUONG TUAN KIET',   NULL),
(31, 0, 10, 41, 38,   52000, N'LEGIT',           52000, N'THAI HOA BINH',     NULL),
(32, 0, 10, 41, 50,   35000, N'BURST_RATE',      35000, N'QUACH TUONG VY',    NULL);

DECLARE @cashier_id INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');
DECLARE @auditor_id INT = (SELECT user_id FROM Users WHERE user_code = N'dung_auditor');
DECLARE @speaker_id INT = (SELECT TOP 1 device_id FROM Devices WHERE device_type = N'SPEAKER' ORDER BY device_id);

-- The cashier creates each order about 3 minutes before the customer pays; the QR lives 15 minutes.
INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at, created_at)
SELECT N'TINKY' + CAST(20100 + n AS NVARCHAR(10)),
       @cashier_id,
       N'nam_cashier',
       order_amount,
       CASE WHEN kind = N'AMOUNT_MISMATCH' THEN N'PENDING' ELSE N'PAID' END,
       N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount='
         + CAST(CAST(order_amount AS BIGINT) AS NVARCHAR(20))
         + N'&addInfo=TINKY' + CAST(20100 + n AS NVARCHAR(10))
         + N'&accountName=LAM%20HOANG%20AN',
       DATEADD(MINUTE, 12, DATEADD(SECOND, hh * 3600 + mm * 60 + ss, CAST(CAST(DATEADD(DAY, -days_ago, SYSDATETIME()) AS DATE) AS DATETIME2))),
       DATEADD(MINUTE, -3, DATEADD(SECOND, hh * 3600 + mm * 60 + ss, CAST(CAST(DATEADD(DAY, -days_ago, SYSDATETIME()) AS DATE) AS DATETIME2)))
FROM @demo
WHERE order_amount IS NOT NULL;

INSERT INTO Transactions (bank_trans_id, order_id, device_id, amount, content, bank_account, gateway, status, trans_time, received_at, announced, printed)
SELECT N'FT26' + CAST(10200000 + d.n * 3917 AS NVARCHAR(12)),
       o.order_id,
       @speaker_id,
       d.amount,
       COALESCE(d.content, d.cname + N' chuyen tien TINKY' + CAST(20100 + d.n AS NVARCHAR(10))),
       N'77884268888',
       N'SEPAY',
       CASE WHEN d.kind IN (N'LEGIT') THEN N'LEGIT'
            WHEN d.kind = N'DUP_TRANS' THEN N'DUPLICATE'
            WHEN d.kind IN (N'HIGH_AMOUNT', N'BURST_RATE') THEN N'SUSPECTED_FRAUD'
            ELSE N'AMOUNT_MISMATCH' END,
       DATEADD(SECOND, d.hh * 3600 + d.mm * 60 + d.ss, CAST(CAST(DATEADD(DAY, -d.days_ago, SYSDATETIME()) AS DATE) AS DATETIME2)),
       DATEADD(SECOND, d.hh * 3600 + d.mm * 60 + d.ss + 6, CAST(CAST(DATEADD(DAY, -d.days_ago, SYSDATETIME()) AS DATE) AS DATETIME2)),
       1, 0
FROM @demo d
LEFT JOIN Orders o ON o.order_code = N'TINKY' + CAST(20100 + d.n AS NVARCHAR(10));

INSERT INTO FraudAlerts (trans_id, rule_code, reason, severity, status, resolved_by, resolved_notes, created_at)
SELECT t.trans_id,
       CASE d.kind WHEN N'NO_ORDER' THEN N'NO_ORDER' WHEN N'DUP_TRANS' THEN N'DUP_TRANS'
                   WHEN N'AMOUNT_MISMATCH' THEN N'AMOUNT_MISMATCH' WHEN N'HIGH_AMOUNT' THEN N'HIGH_AMOUNT'
                   ELSE N'BURST_RATE' END,
       CASE d.kind
            WHEN N'NO_ORDER' THEN N'No order matches the code in the transfer content.'
            WHEN N'DUP_TRANS' THEN N'A transaction with the same amount and content arrived within the last 60 seconds.'
            WHEN N'AMOUNT_MISMATCH' THEN
                 CASE WHEN d.amount < d.order_amount
                      THEN N'Order TINKY' + CAST(20100 + d.n AS NVARCHAR(10)) + N' needs ' + CAST(CAST(d.order_amount AS BIGINT) AS NVARCHAR(20))
                           + N' VND but ' + CAST(CAST(d.amount AS BIGINT) AS NVARCHAR(20)) + N' VND was received (short by '
                           + CAST(CAST(d.order_amount - d.amount AS BIGINT) AS NVARCHAR(20)) + N' VND).'
                      ELSE N'Order TINKY' + CAST(20100 + d.n AS NVARCHAR(10)) + N' needs ' + CAST(CAST(d.order_amount AS BIGINT) AS NVARCHAR(20))
                           + N' VND but ' + CAST(CAST(d.amount AS BIGINT) AS NVARCHAR(20)) + N' VND was received (over by '
                           + CAST(CAST(d.amount - d.order_amount AS BIGINT) AS NVARCHAR(20)) + N' VND).' END
            WHEN N'HIGH_AMOUNT' THEN N'Amount is 5000000 VND or more and needs a manual check.'
            ELSE N'5 or more transactions within 60 seconds.' END,
       CASE WHEN d.kind IN (N'HIGH_AMOUNT', N'BURST_RATE') THEN N'HIGH' ELSE N'MEDIUM' END,
       CASE d.n WHEN 14 THEN N'DISMISSED' WHEN 25 THEN N'RESOLVED' ELSE N'NEW' END,
       CASE WHEN d.n IN (14, 25) THEN @auditor_id ELSE NULL END,
       CASE d.n WHEN 14 THEN N'Customer sent it twice by mistake, no fraud.'
                WHEN 25 THEN N'Customer paid the difference in cash.' ELSE NULL END,
       t.received_at
FROM @demo d
JOIN Transactions t ON t.bank_trans_id = N'FT26' + CAST(10200000 + d.n * 3917 AS NVARCHAR(12))
WHERE d.kind <> N'LEGIT';
GO

PRINT N'>>> Database and sample data for PaymentSpeaker loaded successfully!';
GO
SET NOEXEC OFF;
GO
