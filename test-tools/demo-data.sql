-- Demo data for the transactions page, chart and alert center. Run AFTER 01_schema.sql and 02_seed.sql.
-- Safe to run again: it removes its own rows first (bank ids DEMO..., order codes DEMO0001...).
USE PaymentSpeaker;
GO

DELETE FROM FraudAlerts WHERE trans_id IN (SELECT trans_id FROM Transactions WHERE bank_trans_id LIKE N'DEMO%');
DELETE FROM Transactions WHERE bank_trans_id LIKE N'DEMO%';
DELETE FROM Orders WHERE order_code LIKE N'DEMO%';
GO

DECLARE @demo TABLE (
    n INT, days_ago INT, hh INT, amount DECIMAL(18,2), kind NVARCHAR(30),
    order_amount DECIMAL(18,2), content NVARCHAR(200)
);

INSERT INTO @demo (n, days_ago, hh, amount, kind, order_amount, content) VALUES
(1, 6, 9, 50000, N'LEGIT', 50000, NULL),
(2, 6, 11, 120000, N'LEGIT', 120000, NULL),
(3, 6, 15, 80000, N'LEGIT', 80000, NULL),
(4, 5, 9, 65000, N'LEGIT', 65000, NULL),
(5, 5, 13, 200000, N'LEGIT', 200000, NULL),
(6, 5, 16, 45000, N'LEGIT', 45000, NULL),
(7, 5, 17, 150000, N'NO_ORDER', NULL, N'Unknown transfer'),
(8, 4, 10, 90000, N'LEGIT', 90000, NULL),
(9, 4, 12, 300000, N'LEGIT', 300000, NULL),
(10, 4, 14, 75000, N'LEGIT', 75000, NULL),
(11, 4, 15, 75000, N'AMOUNT_MISMATCH', 80000, NULL),
(12, 3, 9, 110000, N'LEGIT', 110000, NULL),
(13, 3, 11, 60000, N'LEGIT', 60000, NULL),
(14, 3, 12, 60000, N'NO_ORDER', NULL, N'Coffee payment'),
(15, 3, 12, 60000, N'DUP_TRANS', NULL, N'Coffee payment'),
(16, 3, 16, 6500000, N'HIGH_AMOUNT', 6500000, NULL),
(17, 2, 9, 70000, N'LEGIT', 70000, NULL),
(18, 2, 10, 130000, N'LEGIT', 130000, NULL),
(19, 2, 14, 95000, N'LEGIT', 95000, NULL),
(20, 2, 16, 55000, N'LEGIT', 55000, NULL),
(21, 1, 9, 85000, N'LEGIT', 85000, NULL),
(22, 1, 11, 140000, N'LEGIT', 140000, NULL),
(23, 1, 13, 60000, N'LEGIT', 60000, NULL),
(24, 1, 15, 7200000, N'HIGH_AMOUNT', 7200000, NULL),
(25, 1, 18, 50000, N'AMOUNT_MISMATCH', 45000, NULL),
(26, 0, 9, 100000, N'LEGIT', 100000, NULL),
(27, 0, 10, 120000, N'LEGIT', 120000, NULL),
(28, 0, 11, 35000, N'BURST_RATE', 35000, NULL);

INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at, created_at)
SELECT N'DEMO' + RIGHT(N'0000' + CAST(n AS NVARCHAR(4)), 4), 3, N'nam_cashier', order_amount,
       CASE WHEN kind = N'LEGIT' THEN N'PAID' ELSE N'PENDING' END, NULL,
       DATEADD(MINUTE, 10, DATEADD(HOUR, hh, CAST(CAST(DATEADD(DAY, -days_ago, SYSDATETIME()) AS DATE) AS DATETIME2))),
       DATEADD(HOUR, hh, CAST(CAST(DATEADD(DAY, -days_ago, SYSDATETIME()) AS DATE) AS DATETIME2))
FROM @demo
WHERE order_amount IS NOT NULL;

INSERT INTO Transactions (bank_trans_id, order_id, device_id, amount, content, bank_account, gateway, status, trans_time, received_at, announced, printed)
SELECT N'DEMO' + RIGHT(N'0000' + CAST(d.n AS NVARCHAR(4)), 4),
       o.order_id,
       (SELECT TOP 1 v.device_id FROM Devices v WHERE v.device_type = N'SPEAKER' ORDER BY v.device_id),
       d.amount,
       COALESCE(d.content, N'DEMO' + RIGHT(N'0000' + CAST(d.n AS NVARCHAR(4)), 4) + N' payment'),
       N'55987654321',
       N'SEPAY',
       CASE WHEN d.kind = N'LEGIT' THEN N'LEGIT'
            WHEN d.kind = N'DUP_TRANS' THEN N'DUPLICATE'
            WHEN d.kind IN (N'HIGH_AMOUNT', N'BURST_RATE') THEN N'SUSPECTED_FRAUD'
            ELSE N'AMOUNT_MISMATCH' END,
       DATEADD(MINUTE, d.n, DATEADD(HOUR, d.hh, CAST(CAST(DATEADD(DAY, -d.days_ago, SYSDATETIME()) AS DATE) AS DATETIME2))),
       DATEADD(MINUTE, d.n, DATEADD(HOUR, d.hh, CAST(CAST(DATEADD(DAY, -d.days_ago, SYSDATETIME()) AS DATE) AS DATETIME2))),
       1, 0
FROM @demo d
LEFT JOIN Orders o ON o.order_code = N'DEMO' + RIGHT(N'0000' + CAST(d.n AS NVARCHAR(4)), 4);

INSERT INTO FraudAlerts (trans_id, rule_code, reason, severity, status, resolved_by, resolved_notes, created_at)
SELECT t.trans_id,
       CASE d.kind WHEN N'NO_ORDER' THEN N'NO_ORDER' WHEN N'DUP_TRANS' THEN N'DUP_TRANS'
                   WHEN N'AMOUNT_MISMATCH' THEN N'AMOUNT_MISMATCH' WHEN N'HIGH_AMOUNT' THEN N'HIGH_AMOUNT'
                   ELSE N'BURST_RATE' END,
       CASE d.kind
            WHEN N'NO_ORDER' THEN N'No order matches the code in the transfer content.'
            WHEN N'DUP_TRANS' THEN N'A transaction with the same amount and content arrived within the last 60 seconds.'
            WHEN N'AMOUNT_MISMATCH' THEN N'The amount received does not match the order amount.'
            WHEN N'HIGH_AMOUNT' THEN N'Amount is 5000000 VND or more and needs a manual check.'
            ELSE N'5 or more transactions within 60 seconds.' END,
       CASE WHEN d.kind IN (N'HIGH_AMOUNT', N'BURST_RATE') THEN N'HIGH' ELSE N'MEDIUM' END,
       CASE d.n WHEN 14 THEN N'DISMISSED' WHEN 25 THEN N'RESOLVED' ELSE N'NEW' END,
       CASE WHEN d.n IN (14, 25) THEN 4 ELSE NULL END,
       CASE d.n WHEN 14 THEN N'Customer sent it twice by mistake, no fraud.'
                WHEN 25 THEN N'Customer paid the difference in cash.' ELSE NULL END,
       t.received_at
FROM @demo d
JOIN Transactions t ON t.bank_trans_id = N'DEMO' + RIGHT(N'0000' + CAST(d.n AS NVARCHAR(4)), 4)
WHERE d.kind <> N'LEGIT';
GO

SELECT status, COUNT(*) AS transactions FROM Transactions WHERE bank_trans_id LIKE N'DEMO%' GROUP BY status;
SELECT a.status, COUNT(*) AS alerts FROM FraudAlerts a JOIN Transactions t ON t.trans_id = a.trans_id
WHERE t.bank_trans_id LIKE N'DEMO%' GROUP BY a.status;
GO
