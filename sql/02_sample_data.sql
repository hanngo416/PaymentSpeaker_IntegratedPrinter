
USE PaymentSpeakerDB;
GO

INSERT INTO Store (store_code, store_name, address, phone, bank_account, status) VALUES
    (N'HCM01', N'Cua hang Quan 1',   N'12 Le Loi, Quan 1, TP HCM', N'0901000101', N'0011223344', 'ACTIVE'),
    (N'HN01',  N'Cua hang Cau Giay', N'88 Tran Thai Tong, Ha Noi', N'0901000102', N'0055667788', 'ACTIVE'),
    (N'DN01',  N'Cua hang Hai Chau', N'5 Bach Dang, Da Nang',      N'0901000103', N'0099887766', 'INACTIVE');
GO

INSERT INTO Roles (role_name) VALUES
    (N'ADMIN'), (N'STORE_MANAGER'), (N'CASHIER'), (N'TECHNICIAN'), (N'AUDITOR');
GO

INSERT INTO Permissions (resource, action)
SELECT r.res, a.act
FROM (VALUES (N'USER'), (N'ROLE'), (N'DEVICE'), (N'SENSOR_TYPE'), (N'CALIBRATION'), (N'PROFILE'),
             (N'TRANSACTION'), (N'STORE'), (N'ALERT'), (N'MAINTENANCE'), (N'AUDIT')) r(res)
CROSS JOIN (VALUES (N'READ'), (N'WRITE')) a(act);
GO

INSERT INTO RolePerm (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM Roles r CROSS JOIN Permissions p WHERE r.role_name = N'ADMIN';

INSERT INTO RolePerm (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM Roles r CROSS JOIN Permissions p
WHERE r.role_name = N'AUDITOR' AND p.action = N'READ';

INSERT INTO RolePerm (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM Roles r JOIN Permissions p ON 1 = 1
WHERE r.role_name = N'STORE_MANAGER' AND (
      (p.resource IN (N'USER', N'STORE', N'TRANSACTION', N'DEVICE', N'MAINTENANCE') AND p.action = N'READ')
   OR (p.resource IN (N'ALERT', N'PROFILE')));

INSERT INTO RolePerm (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM Roles r JOIN Permissions p ON 1 = 1
WHERE r.role_name = N'CASHIER' AND p.action = N'READ'
  AND p.resource IN (N'TRANSACTION', N'ALERT', N'DEVICE', N'STORE');

INSERT INTO RolePerm (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM Roles r JOIN Permissions p ON 1 = 1
WHERE r.role_name = N'TECHNICIAN' AND (
      p.resource IN (N'DEVICE', N'SENSOR_TYPE', N'CALIBRATION', N'PROFILE', N'MAINTENANCE')
   OR (p.resource IN (N'ALERT', N'STORE') AND p.action = N'READ'));
GO

INSERT INTO Device (device_code, location, firmware, status, api_key_hash, store_id)
SELECT v.code, v.loc, N'1.0.3', v.st,
       LOWER(CONVERT(CHAR(64), HASHBYTES('SHA2_256', v.code + N'-api-key'), 2)), s.store_id
FROM (VALUES
    (N'SPK-HCM01-01', N'Quay thu ngan 1', N'ACTIVE',      N'HCM01'),
    (N'SPK-HCM01-02', N'Quay thu ngan 2', N'MAINTENANCE', N'HCM01'),
    (N'SPK-HN01-01',  N'Quay thu ngan',   N'ACTIVE',      N'HN01')
) v(code, loc, st, sc)
JOIN Store s ON s.store_code = v.sc;
GO

INSERT INTO Users (username, password_hash, full_name, status, store_id)
SELECT v.u, v.h, v.fn, v.st, s.store_id
FROM (VALUES
    (N'admin',    '20000:d9d528ccfab35fb3658946b352928f0a:a49e737ef91177f25e5773096e5a98fad9ccbc175d6a3b740b0a951045b8ef30', N'Ngo Ngoc Gia Han',      'ACTIVE', NULL),
    (N'manager1', '20000:1ff3c45e671540125084738fd81dff63:1f518bdaff8ddfcc6e845d90fd28a82bac46f5d5156e4db49e74ea77220b7bf9', N'Tran Thi Ly',       'ACTIVE', N'HCM01'),
    (N'manager2', '20000:2a445de94ecf474c84e9086b8c82b307:de059908699234363664524af0f184d2a1b880c66bb51c10789445d1495ee530', N'Pham Van Hung',     'ACTIVE', N'HN01'),
    (N'cashier1', '20000:7fa336f5e7117427d705c9ef3885880c:f93b24eeb9482620881e563823433167885d6190d7f85eb3fb27c204f0f5b53a', N'Bui Thi Hoa',       'ACTIVE', N'HCM01'),
    (N'cashier2', '20000:c0802bb413c38200c814b29858da8fcc:288a7a17a50149f3ba80d2aefd5dbf83c007979ae335b6c6e1104ab8c3dd55c9', N'Ngo Van Kien',      'ACTIVE', N'HN01'),
    (N'tech1',    '20000:cbd7b721c3fe71272e4e2934ba080cbe:21fa7f463de5180be2caf8de41fc00e60fda2453850a850ac0fdaa41283a1872', N'Le Minh Thu',       'ACTIVE', NULL),
    (N'auditor1', '20000:43ee43ebb66d2ffcd603a621d3074c17:0bc15ca2f67e45e06f522617bbe293fc0c929a2a4c4acd07e5ab1ba4a571d0dd', N'Do Thi Mai',        'ACTIVE', NULL),
    (N'locked1',  '20000:6fa9453c1523973c7bb6e503de3ba9a1:357e0c0d42f8a1dd585a373dc93dbf10abcef18224c88a226e061f370a12e880', N'Tai khoan da khoa', 'LOCKED', N'HCM01')
) v(u, h, fn, st, sc)
LEFT JOIN Store s ON s.store_code = v.sc;
GO

INSERT INTO UserRole (user_id, role_id)
SELECT u.user_id, r.role_id
FROM (VALUES
    (N'admin',    N'ADMIN'),
    (N'manager1', N'STORE_MANAGER'), (N'manager2', N'STORE_MANAGER'),
    (N'cashier1', N'CASHIER'),       (N'cashier2', N'CASHIER'),
    (N'tech1',    N'TECHNICIAN'),
    (N'auditor1', N'AUDITOR'),
    (N'locked1',  N'CASHIER')
) v(u, r)
JOIN Users u ON u.username = v.u
JOIN Roles r ON r.role_name = v.r;
GO

DECLARE @d1 INT = (SELECT device_id FROM Device WHERE device_code = N'SPK-HCM01-01');
DECLARE @s1 INT = (SELECT store_id  FROM Store  WHERE store_code  = N'HCM01');

INSERT INTO PaymentTransaction
    (store_id, device_id, sepay_txn_id, gateway, account_number, amount, transfer_content,
     reference_code, measured_at, txn_min_of_day, expected_amount, gap_seconds_prev)
VALUES
    (@s1, @d1, 900001, N'Vietcombank', N'0011223344',   50000, N'CK mua hang don 101', N'FT26001', DATEADD(HOUR, -5, SYSUTCDATETIME()), 540,  50000,  NULL),
    (@s1, @d1, 900002, N'MB Bank',     N'0011223344',  120000, N'CK mua hang don 102', N'FT26002', DATEADD(HOUR, -4, SYSUTCDATETIME()), 600,  120000, 3600),
    (@s1, @d1, 900003, N'MB Bank',     N'0011223344',  120000, N'CK mua hang don 102', N'FT26002', DATEADD(HOUR, -4, SYSUTCDATETIME()), 601,  120000, 5),
    (@s1, @d1, 900004, N'Techcombank', N'0011223344',   80000, N'CK don 103',          N'FT26003', DATEADD(HOUR, -3, SYSUTCDATETIME()), 660,  100000, 3300),
    (@s1, @d1, 900005, N'ACB',         N'0011223344', 9900000, N'chuyen gap',          N'FT26004', DATEADD(HOUR, -1, SYSUTCDATETIME()), 780,  NULL,   7200);

INSERT INTO TransactionLabel (txn_id, label)
SELECT t.txn_id, v.lb
FROM (VALUES (900001, 'LEGIT'), (900002, 'LEGIT'), (900003, 'DUPLICATE'),
             (900004, 'AMOUNT_MISMATCH'), (900005, 'SUSPECTED_FRAUD')) v(sid, lb)
JOIN PaymentTransaction t ON t.device_id = @d1 AND t.sepay_txn_id = v.sid;

INSERT INTO NotificationLog (txn_id, device_id, channel, message_text, status, completed_at)
SELECT t.txn_id, @d1, c.ch,
       N'Da nhan ' + CAST(CAST(t.amount AS BIGINT) AS NVARCHAR(20)) + N' dong', 'DONE', SYSUTCDATETIME()
FROM PaymentTransaction t CROSS JOIN (VALUES ('SPEAKER'), ('PRINTER')) c(ch)
WHERE t.sepay_txn_id IN (900001, 900002);
GO

DECLARE @admin INT = (SELECT user_id FROM Users WHERE username = N'admin');
EXEC Audit_Append @admin, N'CREATE', N'USER', @admin, N'{"username":"manager1","role":"STORE_MANAGER"}';
EXEC Audit_Append @admin, N'UPDATE', N'USER', @admin, N'{"username":"cashier1","store":"HCM01"}';
EXEC Audit_Append @admin, N'LOCK',   N'USER', @admin, N'{"username":"locked1","status":"LOCKED"}';
GO

SELECT r.role_name, COUNT(DISTINCT ur.user_id) AS users, COUNT(DISTINCT rp.perm_id) AS permissions
FROM Roles r
LEFT JOIN UserRole ur ON ur.role_id = r.role_id
LEFT JOIN RolePerm rp ON rp.role_id = r.role_id
GROUP BY r.role_name, r.role_id ORDER BY r.role_id;

SELECT (SELECT COUNT(*) FROM Users) AS users, (SELECT COUNT(*) FROM Store) AS stores,
       (SELECT COUNT(*) FROM Device) AS devices, (SELECT COUNT(*) FROM PaymentTransaction) AS txns,
       (SELECT COUNT(*) FROM HashChain) AS chain_links;
GO
