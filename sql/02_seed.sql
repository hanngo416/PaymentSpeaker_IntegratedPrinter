USE PaymentSpeaker;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

INSERT INTO Roles (role_id, role_name, description)
SELECT v.role_id, v.role_name, v.description FROM (VALUES
(1, N'ADMIN', N'System administrator: full configuration and staff management rights'),
(2, N'STORE_MANAGER', N'Store manager: manages revenue and cashier accounts'),
(3, N'CASHIER', N'Counter cashier: creates orders, sees the payment popup and prints receipts'),
(4, N'AUDITOR', N'Auditor: investigates fraudulent transactions and checks data integrity'),
(5, N'TECHNICIAN', N'Technician: maintains the printer hardware'),
(6, N'AFFILIATE', N'Affiliate: sees only own referrals, commissions and payouts')
) AS v(role_id, role_name, description)
WHERE NOT EXISTS (SELECT 1 FROM Roles r WHERE r.role_id = v.role_id);
GO

INSERT INTO Users (user_code, full_name, email, role_id, status)
SELECT v.user_code, v.full_name, v.email, v.role_id, v.status FROM (VALUES
(N'han_admin', N'Ngo Ngoc Gia Han', N'hanngo816@gmail.com', 1, N'ACTIVE'),
(N'mai_manager', N'Do Thi Mai', N'hanngo816+manager@gmail.com', 2, N'ACTIVE'),
(N'nam_cashier', N'Bui Van Nam', N'hanngo816+cashier@gmail.com', 3, N'ACTIVE'),
(N'dung_auditor', N'Pham Quoc Dung', N'hanngo816+auditor@gmail.com', 4, N'ACTIVE'),
(N'em_technician', N'Hoang Van Em', N'hanngo816+tech@gmail.com', 5, N'ACTIVE'),
(N'ha_cashier', N'Vu Thi Ha', N'hanngo816+locked@gmail.com', 3, N'LOCKED'),
(N'linh_affiliate', N'Linh Review', N'hanngo816+linh@gmail.com', 6, N'ACTIVE')
) AS v(user_code, full_name, email, role_id, status)
WHERE NOT EXISTS (SELECT 1 FROM Users u WHERE u.user_code = v.user_code OR u.email = v.email);
GO

INSERT INTO Devices (device_code, device_type, model_name, api_key, status)
SELECT N'PRN_01', N'PRINTER', N'Aiyin A70 Thermal 80mm', N'KEY_PRN_01_SECRET_5432', N'ONLINE'
WHERE NOT EXISTS (SELECT 1 FROM Devices WHERE device_code = N'PRN_01');
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT N'vat_rate', N'8'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'vat_rate');
INSERT INTO Settings (setting_key, setting_value)
SELECT N'vat_included', N'0'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'vat_included');
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT N'printer_name', N'A70'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'printer_name');
GO

INSERT INTO TaxRules (kind, sector, value, effective_from, effective_to, source_ref, verified)
SELECT v.kind, v.sector, v.value, '2026-01-01', v.effective_to, v.source_ref, v.verified
FROM (VALUES
(N'THRESHOLD', NULL, 1000000000, NULL, N'Decree 141/2026/ND-CP Art.1 (amends Decree 68/2026/ND-CP Art.3,4)', 1),
(N'PIT_CEILING', NULL, 3000000000, NULL, N'Decree 68/2026/ND-CP Art.4 clause 5', 1),
(N'WARN_LEVEL', NULL, 70, NULL, N'Tinky internal warning level', 1),
(N'WARN_LEVEL', NULL, 90, NULL, N'Tinky internal warning level', 1),
(N'WARN_LEVEL', NULL, 100, NULL, N'Tinky internal warning level', 1),
(N'VAT_RATE', N'GOODS', 1, NULL, N'MOF Q&A citing Circular 40/2021/TT-BTC', 0),
(N'VAT_RATE', N'SERVICE', 5, NULL, N'MOF Q&A citing Circular 40/2021/TT-BTC', 0),
(N'VAT_RATE', N'PRODUCTION', 3, NULL, N'MOF Q&A citing Circular 40/2021/TT-BTC', 0),
(N'VAT_RATE', N'OTHER', 2, NULL, N'MOF Q&A citing Circular 40/2021/TT-BTC', 0),
(N'PIT_RATE', N'GOODS', 0.5, NULL, N'PIT Law 109/2025/QH15 Art.7 clause 3', 0),
(N'PIT_RATE', N'SERVICE', 2, NULL, N'PIT Law 109/2025/QH15 Art.7 clause 3', 0),
(N'PIT_RATE', N'PRODUCTION', 1.5, NULL, N'PIT Law 109/2025/QH15 Art.7 clause 3', 0),
(N'PIT_RATE', N'OTHER', 1, NULL, N'PIT Law 109/2025/QH15 Art.7 clause 3', 0),
(N'VAT_REDUCTION', NULL, 20, '2026-12-31', N'Resolution 204/2025/QH15', 0)
) AS v (kind, sector, value, effective_to, source_ref, verified)
WHERE NOT EXISTS (
    SELECT 1 FROM TaxRules t
    WHERE t.kind = v.kind AND t.effective_from = '2026-01-01'
      AND (t.sector = v.sector OR (t.sector IS NULL AND v.sector IS NULL))
      AND (v.kind <> N'WARN_LEVEL' OR t.value = v.value)
);
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT N'tax_sector', N'GOODS'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'tax_sector');
GO

INSERT INTO DiscountCodes (code, kind, value, holder, max_uses, valid_from, valid_to)
SELECT v.code, v.kind, v.value, v.holder, v.max_uses, v.valid_from, v.valid_to FROM (VALUES
(N'SUMMER10', N'PERCENT', 10, NULL, NULL, '2026-10-01', '2026-12-31'),
(N'WELCOME20K', N'FIXED', 20000, NULL, 200, '2026-10-01', '2026-12-31'),
(N'TINKY123', N'PERCENT', 15, N'Nguyen Van An', 1, '2026-10-01', '2026-12-31')
) AS v(code, kind, value, holder, max_uses, valid_from, valid_to)
WHERE NOT EXISTS (SELECT 1 FROM DiscountCodes d WHERE d.code = v.code);
GO

INSERT INTO Categories (category_name, display_order)
SELECT v.category_name, v.display_order FROM (VALUES
(N'Beverages', 1),
(N'Snacks', 2),
(N'Bakery', 3),
(N'Ready to Eat', 4)
) AS v(category_name, display_order)
WHERE NOT EXISTS (SELECT 1 FROM Categories c WHERE c.category_name = v.category_name);
GO

DECLARE @cat_bev INT = (SELECT category_id FROM Categories WHERE category_name = N'Beverages');
DECLARE @cat_snk INT = (SELECT category_id FROM Categories WHERE category_name = N'Snacks');
DECLARE @cat_bak INT = (SELECT category_id FROM Categories WHERE category_name = N'Bakery');
DECLARE @cat_rte INT = (SELECT category_id FROM Categories WHERE category_name = N'Ready to Eat');

INSERT INTO [Groups] (category_id, group_name, display_order)
SELECT v.category_id, v.group_name, v.display_order FROM (VALUES
(@cat_bev, N'Soft Drinks', 1),
(@cat_bev, N'Energy Drinks', 2),
(@cat_bev, N'Water & Tea', 3),
(@cat_bev, N'Canned Coffee', 4),
(@cat_snk, N'Lay''s Chips', 1),
(@cat_snk, N'Oishi Snacks', 2),
(@cat_snk, N'Orion & Rice', 3),
(@cat_snk, N'Dried Treats', 4),
(@cat_bak, N'Sweet Bakery', 1),
(@cat_bak, N'Savory Bakery', 2),
(@cat_rte, N'Instant Noodles', 1),
(@cat_rte, N'Sausages', 2)
) AS v(category_id, group_name, display_order)
WHERE NOT EXISTS (SELECT 1 FROM [Groups] g WHERE g.category_id = v.category_id AND g.group_name = v.group_name);
GO

DECLARE @grp_soft INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Soft Drinks');
DECLARE @grp_ener INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Energy Drinks');
DECLARE @grp_watr INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Water & Tea');
DECLARE @grp_coff INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Canned Coffee');
DECLARE @grp_lays INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Lay''s Chips');
DECLARE @grp_oish INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Oishi Snacks');
DECLARE @grp_orin INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Orion & Rice');
DECLARE @grp_drie INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Dried Treats');
DECLARE @grp_swtb INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Sweet Bakery');
DECLARE @grp_savb INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Savory Bakery');
DECLARE @grp_nood INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Instant Noodles');
DECLARE @grp_saus INT = (SELECT group_id FROM [Groups] WHERE group_name = N'Sausages');

INSERT INTO Products (group_id, product_code, product_name, price, active, display_order)
SELECT v.group_id, v.product_code, v.product_name, v.price, 1, v.display_order FROM (VALUES
(@grp_soft, N'DRK01', N'Coca-Cola Original 330ml', 12000.00, 1),
(@grp_soft, N'DRK02', N'Pepsi Zero Lime 330ml', 12000.00, 2),
(@grp_soft, N'DRK03', N'7Up Revive Salted Lemon 500ml', 11000.00, 3),
(@grp_soft, N'DRK04', N'Mirinda Cream Soda 330ml', 11000.00, 4),
(@grp_ener, N'DRK05', N'Red Bull Gold Can 250ml', 15000.00, 1),
(@grp_ener, N'DRK06', N'Sting Strawberry 330ml', 12000.00, 2),
(@grp_ener, N'DRK07', N'Monster Energy Green 355ml', 32000.00, 3),
(@grp_watr, N'DRK08', N'Aquafina Water 500ml', 7000.00, 1),
(@grp_watr, N'DRK09', N'C2 Green Tea Lemon 455ml', 10000.00, 2),
(@grp_watr, N'DRK10', N'Tea+ Oolong Tea 450ml', 11000.00, 3),
(@grp_coff, N'DRK11', N'Highlands Coffee Can 235ml', 17000.00, 1),
(@grp_coff, N'DRK12', N'Nescafe Iced Black 180ml', 14000.00, 2),
(@grp_lays, N'SNK01', N'Lay''s Classic Salt 54g', 18000.00, 1),
(@grp_lays, N'SNK02', N'Lay''s Nori Seaweed 54g', 18000.00, 2),
(@grp_lays, N'SNK03', N'Lay''s BBQ Pork 54g', 18000.00, 3),
(@grp_oish, N'SNK04', N'Oishi Crab Flavor 40g', 8000.00, 1),
(@grp_oish, N'SNK05', N'Oishi Tom Yum Shrimp 40g', 8000.00, 2),
(@grp_oish, N'SNK06', N'Oishi Pillows Choco 100g', 16000.00, 3),
(@grp_orin, N'SNK07', N'Swing Beef Steak 54g', 18000.00, 1),
(@grp_orin, N'SNK08', N'O''Star Seaweed 54g', 18000.00, 2),
(@grp_orin, N'SNK09', N'An Rice Crackers 150g', 22000.00, 3),
(@grp_drie, N'SNK10', N'Crispy Shredded Chicken 50g', 25000.00, 1),
(@grp_drie, N'SNK11', N'Bento Spicy Squid 20g', 16000.00, 2),
(@grp_swtb, N'BAK01', N'Butter Croissant 60g', 22000.00, 1),
(@grp_swtb, N'BAK02', N'Chocolate Donut', 18000.00, 2),
(@grp_swtb, N'BAK03', N'Custard Cream Bun', 15000.00, 3),
(@grp_savb, N'BAK04', N'Pork Floss Sandwich', 25000.00, 1),
(@grp_savb, N'BAK05', N'Sausage Roll Bread', 20000.00, 2),
(@grp_nood, N'RTE01', N'Omachi Beef Stew Cup', 16000.00, 1),
(@grp_nood, N'RTE02', N'Hao Hao Sour Shrimp Cup', 10000.00, 2),
(@grp_nood, N'RTE03', N'Shin Ramyun Cup', 22000.00, 3),
(@grp_saus, N'RTE04', N'CP Smoked Sausage 100g', 18000.00, 1),
(@grp_saus, N'RTE05', N'Vissan Pork Sausage 4x17g', 14000.00, 2)
) AS v(group_id, product_code, product_name, price, display_order)
WHERE NOT EXISTS (SELECT 1 FROM Products p WHERE p.product_code = v.product_code);
GO

INSERT INTO Categories (category_name, display_order)
SELECT N'Demo Menu', 5
WHERE NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Demo Menu');
GO

DECLARE @cat_demo INT = (SELECT category_id FROM Categories WHERE category_name = N'Demo Menu');
INSERT INTO [Groups] (category_id, group_name, display_order)
SELECT v.category_id, v.group_name, v.display_order FROM (VALUES
(@cat_demo, N'Tiny Snacks', 1),
(@cat_demo, N'Cheap Drinks', 2),
(@cat_demo, N'Street Food', 3)
) AS v(category_id, group_name, display_order)
WHERE NOT EXISTS (SELECT 1 FROM [Groups] g WHERE g.category_id = v.category_id AND g.group_name = v.group_name);
GO

DECLARE @cat_demo2 INT = (SELECT category_id FROM Categories WHERE category_name = N'Demo Menu');
DECLARE @grp_tiny INT = (SELECT group_id FROM [Groups] WHERE category_id = @cat_demo2 AND group_name = N'Tiny Snacks');
DECLARE @grp_cdrk INT = (SELECT group_id FROM [Groups] WHERE category_id = @cat_demo2 AND group_name = N'Cheap Drinks');
DECLARE @grp_strt INT = (SELECT group_id FROM [Groups] WHERE category_id = @cat_demo2 AND group_name = N'Street Food');

INSERT INTO Products (group_id, product_code, product_name, price, active, display_order)
SELECT v.group_id, v.product_code, v.product_name, v.price, 1, v.display_order FROM (VALUES
(@grp_tiny, N'DEMO01', N'Bubble Gum Stick', 500.00, 1),
(@grp_tiny, N'DEMO02', N'Strawberry Candy', 500.00, 2),
(@grp_tiny, N'DEMO03', N'Lollipop', 1000.00, 3),
(@grp_tiny, N'DEMO04', N'Roasted Peanut Pack', 1500.00, 4),
(@grp_tiny, N'DEMO05', N'Mini Rice Cracker', 2000.00, 5),
(@grp_tiny, N'DEMO06', N'Fruit Jelly Cup', 3000.00, 6),
(@grp_cdrk, N'DEMO07', N'Cup of Ice', 500.00, 1),
(@grp_cdrk, N'DEMO08', N'Iced Tea Cup', 2000.00, 2),
(@grp_cdrk, N'DEMO09', N'Soy Milk Bag', 3000.00, 3),
(@grp_cdrk, N'DEMO10', N'Lemon Tea Cup', 5000.00, 4),
(@grp_cdrk, N'DEMO11', N'Sugarcane Juice Cup', 7000.00, 5),
(@grp_cdrk, N'DEMO12', N'Coconut Water Cup', 10000.00, 6),
(@grp_strt, N'DEMO13', N'Boiled Quail Egg', 1000.00, 1),
(@grp_strt, N'DEMO14', N'Fried Dumpling', 4000.00, 2),
(@grp_strt, N'DEMO15', N'Grilled Corn', 5000.00, 3),
(@grp_strt, N'DEMO16', N'Rice Paper Roll', 6000.00, 4),
(@grp_strt, N'DEMO17', N'Plain Banh Mi', 8000.00, 5),
(@grp_strt, N'DEMO18', N'Sticky Rice Bag', 10000.00, 6)
) AS v(group_id, product_code, product_name, price, display_order)
WHERE NOT EXISTS (SELECT 1 FROM Products p WHERE p.product_code = v.product_code);
GO

UPDATE Products SET cost_price = ROUND(price * 0.6, 0) WHERE cost_price = 0;
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT N'sell_channels', N'STORE'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'sell_channels');
INSERT INTO Settings (setting_key, setting_value)
SELECT N'onboarded', N'0'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'onboarded');
GO

INSERT INTO Affiliates (name, rate, active)
SELECT v.name, v.rate, 1 FROM (VALUES
(N'Linh Review', 8.00),
(N'Minh Unbox', 5.00),
(N'Hana Daily', 10.00)
) AS v(name, rate)
WHERE NOT EXISTS (SELECT 1 FROM Affiliates a WHERE a.name = v.name);
GO

UPDATE Affiliates SET referral_code = CASE name WHEN N'Linh Review' THEN N'LINH08' WHEN N'Minh Unbox' THEN N'MINH05' WHEN N'Hana Daily' THEN N'HANA10' END
WHERE referral_code IS NULL AND name IN (N'Linh Review', N'Minh Unbox', N'Hana Daily');
UPDATE Affiliates SET email = N'hanngo816+linh@gmail.com', phone = N'0903112481', bank_name = N'Vietcombank', bank_account = N'0071001234', bank_holder = N'PHAM THI LINH'
WHERE name = N'Linh Review' AND email IS NULL;
UPDATE Affiliates SET email = N'minh.unbox@example.com', phone = N'0912778204', bank_name = N'TPBank', bank_account = N'0213445589', bank_holder = N'NGUYEN VAN MINH'
WHERE name = N'Minh Unbox' AND email IS NULL;
GO

INSERT INTO Affiliates (name, rate, referral_code, phone, email, bank_name, bank_account, bank_holder, status, active)
SELECT v.name, 0, v.referral_code, v.phone, v.email, v.bank_name, v.bank_account, v.bank_holder, v.status,
       CASE WHEN v.status = N'ACTIVE' THEN 1 ELSE 0 END
FROM (VALUES
(N'Bao Chau', N'BAO31', N'0944100233', N'bao.chau@example.com', NULL, NULL, NULL, N'PENDING'),
(N'Quan Bui', N'QUAN99', N'0905667118', N'quan.bui@example.com', N'VPBank', N'3300118820', N'BUI MINH QUAN', N'SUSPENDED')
) AS v(name, referral_code, phone, email, bank_name, bank_account, bank_holder, status)
WHERE NOT EXISTS (SELECT 1 FROM Affiliates a WHERE a.name = v.name);
GO

INSERT INTO Campaigns (name, description, commission_type, commission_value, min_order, start_date, end_date, status, created_by)
SELECT v.name, v.description, v.commission_type, v.commission_value, v.min_order,
       CAST(DATEADD(DAY, v.start_off, GETDATE()) AS DATE), CAST(DATEADD(DAY, v.end_off, GETDATE()) AS DATE), v.status,
       (SELECT user_id FROM Users WHERE user_code = N'han_admin')
FROM (VALUES
(N'Autumn Push', N'Percent commission for every referred order.', N'PERCENT', 10.00, 50000.00, -30, 60, N'ACTIVE'),
(N'Weekend Fixed Bonus', N'A fixed bonus for every referred order.', N'FIXED', 10000.00, 50000.00, -30, 90, N'ACTIVE'),
(N'Holiday Draft', N'Planned holiday campaign, not published.', N'PERCENT', 12.00, 150000.00, 40, 100, N'DRAFT')
) AS v(name, description, commission_type, commission_value, min_order, start_off, end_off, status)
WHERE NOT EXISTS (SELECT 1 FROM Campaigns c WHERE c.name = v.name);
GO


IF NOT EXISTS (SELECT 1 FROM ChannelSales)
BEGIN
    DECLARE @admin_id INT = (SELECT user_id FROM Users WHERE user_code = N'han_admin');
    DECLARE @aff1 INT = (SELECT affiliate_id FROM Affiliates WHERE name = N'Linh Review');
    DECLARE @aff2 INT = (SELECT affiliate_id FROM Affiliates WHERE name = N'Minh Unbox');
    DECLARE @aff3 INT = (SELECT affiliate_id FROM Affiliates WHERE name = N'Hana Daily');
    DECLARE @d INT = 13;
    DECLARE @i INT;
    DECLARE @channel NVARCHAR(20);
    DECLARE @goods DECIMAL(18,2);
    DECLARE @fee DECIMAL(18,2);
    DECLARE @aff INT;
    DECLARE @rate DECIMAL(5,2);
    DECLARE @status NVARCHAR(20);
    DECLARE @cost DECIMAL(18,2);
    DECLARE @day DATETIME2(3);
    WHILE @d >= 0
    BEGIN
        SET @day = CAST(CAST(DATEADD(DAY, -@d, SYSDATETIME()) AS DATE) AS DATETIME2(3));
        SET @i = 1;
        WHILE @i <= 4
        BEGIN
            SET @channel = CASE WHEN @i % 2 = 1 THEN N'SHOPEE' ELSE N'TIKTOK' END;
            SET @goods = 150000 + ((@d * 7 + @i * 13) % 10) * 40000;
            SET @fee = ROUND(@goods * CASE WHEN @channel = N'SHOPEE' THEN 0.08 ELSE 0.06 END, 0);
            SET @aff = CASE (@d + @i) % 3 WHEN 0 THEN @aff1 WHEN 1 THEN @aff2 ELSE NULL END;
            SET @rate = CASE WHEN @aff = @aff1 THEN 8 WHEN @aff = @aff2 THEN 5 ELSE 0 END;
            SET @status = CASE
                WHEN (@d = 9 AND @i = 3) OR (@d = 2 AND @i = 1) THEN N'CANCELLED'
                WHEN (@d = 5 AND @i = 2) THEN N'RETURNED'
                ELSE N'COMPLETED' END;
            SET @cost = CASE WHEN @d = 7 THEN ROUND(@goods * 1.3, 0) ELSE ROUND(@goods * 0.6, 0) END;
            INSERT INTO ChannelSales (channel, external_ref, sold_at, status, goods_amount, platform_fee, shipping_fee,
                                      affiliate_id, affiliate_commission, cost_amount, note, created_by)
            VALUES (@channel, CASE WHEN @channel = N'SHOPEE' THEN N'SP-' ELSE N'TT-' END
                              + RIGHT(N'00' + CAST(14 - @d AS NVARCHAR(3)), 2) + N'-' + CAST(@i AS NVARCHAR(2)),
                    DATEADD(HOUR, 8 + @i * 3, @day), @status, @goods, @fee, CASE WHEN @i = 4 THEN 0 ELSE 15000 END,
                    @aff, ROUND(@goods * @rate / 100, 0), @cost,
                    CASE WHEN @d = 7 THEN N'Flash sale below cost' ELSE NULL END, @admin_id);
            SET @i = @i + 1;
        END
        SET @d = @d - 1;
    END
END
GO

IF EXISTS (SELECT 1 FROM Orders) OR EXISTS (SELECT 1 FROM Transactions)
BEGIN
    PRINT N'>>> Orders or transactions already exist - demo data skipped (roles, users, devices and discount codes were checked).';
    SET NOEXEC ON;
END
GO

DECLARE @cashier_id INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');

INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at) VALUES
(N'TINKY10234', @cashier_id, N'nam_cashier', 50000.00, N'PENDING',
 N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=50000&addInfo=TINKY10234&accountName=LAM%20HOANG%20AN',
 DATEADD(MINUTE, 60, SYSDATETIME())),
(N'TINKY10235', @cashier_id, N'nam_cashier', 120000.00, N'PENDING',
 N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=120000&addInfo=TINKY10235&accountName=LAM%20HOANG%20AN',
 DATEADD(MINUTE, 60, SYSDATETIME()));
GO

DECLARE @ord1 INT = (SELECT order_id FROM Orders WHERE order_code = N'TINKY10234');
DECLARE @ord2 INT = (SELECT order_id FROM Orders WHERE order_code = N'TINKY10235');

DECLARE @p_croissant INT = (SELECT product_id FROM Products WHERE product_code = N'BAK01');
DECLARE @p_lays INT = (SELECT product_id FROM Products WHERE product_code = N'SNK01');
DECLARE @p_c2 INT = (SELECT product_id FROM Products WHERE product_code = N'DRK09');
DECLARE @p_roll INT = (SELECT product_id FROM Products WHERE product_code = N'BAK05');

IF @ord1 IS NOT NULL
BEGIN
    INSERT INTO OrderItems (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
    (@ord1, @p_croissant, N'Butter Croissant 60g', 22000.00, 1, 22000.00),
    (@ord1, @p_lays, N'Lay''s Classic Salt 54g', 18000.00, 1, 18000.00),
    (@ord1, @p_c2, N'C2 Green Tea Lemon 455ml', 10000.00, 1, 10000.00);
END

IF @ord2 IS NOT NULL
BEGIN
    INSERT INTO OrderItems (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
    (@ord2, @p_croissant, N'Butter Croissant 60g', 22000.00, 2, 44000.00),
    (@ord2, @p_lays, N'Lay''s Classic Salt 54g', 18000.00, 2, 36000.00),
    (@ord2, @p_roll, N'Sausage Roll Bread', 20000.00, 1, 20000.00),
    (@ord2, @p_c2, N'C2 Green Tea Lemon 455ml', 10000.00, 2, 20000.00);
END
GO

INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, created_at) VALUES
(N'SYSTEM', N'INIT_SYSTEM', N'SYSTEM', N'0', N'{"message": "System initialized."}',
 CAST(CAST(DATEADD(DAY, -6, SYSDATETIME()) AS DATE) AS DATETIME2(3)));
GO

DECLARE @demo TABLE (
    n INT, days_ago INT, hh INT, mm INT, ss INT, amount DECIMAL(18,2), kind NVARCHAR(30),
    order_amount DECIMAL(18,2), cname NVARCHAR(60), content NVARCHAR(200)
);

INSERT INTO @demo (n, days_ago, hh, mm, ss, amount, kind, order_amount, cname, content) VALUES
(1,  6,  8, 12, 41,   45000, N'LEGIT',           45000, N'NGUYEN VAN BINH',   NULL),
(2,  6, 10, 37, 18,  128000, N'LEGIT',          128000, N'TRAN THI LAN',      NULL),
(3,  6, 15,  5, 52,   69000, N'LEGIT',           69000, N'LE MINH KHOA',      NULL),
(4,  5,  9, 23,  7,   89000, N'LEGIT',           89000, N'PHAM THU HA',       NULL),
(5,  5, 12, 48, 33,  215000, N'LEGIT',          215000, N'VO HOANG LONG',     NULL),
(6,  5, 16, 14, 26,   38000, N'LEGIT',           38000, N'DANG QUOC VIET',    NULL),
(7,  5, 17, 31, 59,  150000, N'NO_ORDER',          NULL, N'BUI THANH TUNG',   N'payment for coffee'),
(8,  4, 10,  2, 14,   97000, N'LEGIT',           97000, N'HOANG THI MY',      NULL),
(9,  4, 12, 19, 45,  312000, N'LEGIT',          312000, N'NGO DUC ANH',       NULL),
(10, 4, 14, 41,  3,   76000, N'LEGIT',           76000, N'DO KIM NGAN',       NULL),
(11, 4, 15, 56, 38,   80000, N'LEGIT',           80000, N'LY GIA BAO',        NULL),
(12, 3,  9, 16, 20,  118000, N'LEGIT',          118000, N'TRUONG MY LINH',    NULL),
(13, 3, 11, 44, 51,   62000, N'LEGIT',           62000, N'PHAN VAN DUNG',     NULL),
(14, 3, 12, 33,  9,   60000, N'NO_ORDER',          NULL, N'CAO THI THAO',     N'coffee payment'),
(15, 3, 12, 33, 40,   60000, N'DUP_TRANS',         NULL, N'CAO THI THAO',     N'coffee payment'),
(16, 3, 16, 27, 12, 6500000, N'HIGH_AMOUNT',  6500000, N'CONG TY TNHH MINH PHAT', NULL),
(17, 2,  8, 51, 29,   72000, N'LEGIT',           72000, N'VU THI HONG',       NULL),
(18, 2, 10, 24,  6,  135000, N'LEGIT',          135000, N'DINH VAN SON',      NULL),
(19, 2, 14, 12, 47,   94000, N'LEGIT',           94000, N'LUONG BICH TRAM',   NULL),
(20, 2, 16, 39, 15,   55000, N'LEGIT',           55000, N'NGUYEN HUU PHUOC',  NULL),
(21, 1,  9,  8, 34,   85000, N'LEGIT',           85000, N'TA QUYNH NHU',      NULL),
(22, 1, 11, 52, 22,  146000, N'LEGIT',          146000, N'TRINH CONG HAU',    NULL),
(23, 1, 13, 17, 58,   58000, N'LEGIT',           58000, N'MAI PHUONG ANH',    NULL),
(24, 1, 15, 46,  1, 7200000, N'HIGH_AMOUNT',  7200000, N'CONG TY CP AN KHANG', NULL),
(25, 1, 18,  9, 36,   45000, N'LEGIT',           45000, N'HUYNH NGOC TRAN',   NULL),
(26, 0,  8, 35, 11,  105000, N'LEGIT',          105000, N'LAM GIA KIET',      NULL),
(27, 0,  9, 20, 48,  122000, N'LEGIT',          122000, N'TO THI BICH',       NULL),
(28, 0, 10, 41,  4,   48000, N'LEGIT',           48000, N'VAN THANH DAT',     NULL),
(29, 0, 10, 41, 15,   65000, N'LEGIT',           65000, N'KIEU MY DUYEN',     NULL),
(30, 0, 10, 41, 27,   39000, N'LEGIT',           39000, N'DUONG TUAN KIET',   NULL),
(31, 0, 10, 41, 38,   52000, N'LEGIT',           52000, N'THAI HOA BINH',     NULL),
(32, 0, 10, 41, 50,   35000, N'BURST_RATE',      35000, N'QUACH TUONG VY',    NULL),
(33, 2, 11, 30,  0,   80000, N'AMOUNT_MISMATCH', 90000, N'LAI VAN TAI',       NULL),
(34, 1, 14, 20,  0,   66000, N'ORDER_CLOSED',    66000, N'PHUNG THI OANH',    NULL);

DECLARE @cashier_id INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');
DECLARE @auditor_id INT = (SELECT user_id FROM Users WHERE user_code = N'dung_auditor');

INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at, created_at)
SELECT N'TINKY' + CAST(20100 + n AS NVARCHAR(10)),
       @cashier_id,
       N'nam_cashier',
       order_amount,
       CASE WHEN kind IN (N'AMOUNT_MISMATCH', N'ORDER_CLOSED') THEN N'CANCELLED' ELSE N'COMPLETED' END,
       N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount='
         + CAST(CAST(order_amount AS BIGINT) AS NVARCHAR(20))
         + N'&addInfo=TINKY' + CAST(20100 + n AS NVARCHAR(10))
         + N'&accountName=LAM%20HOANG%20AN',
       DATEADD(MINUTE, CASE WHEN kind = N'ORDER_CLOSED' THEN -5 ELSE 12 END, DATEADD(SECOND, hh * 3600 + mm * 60 + ss, CAST(CAST(DATEADD(DAY, -days_ago, SYSDATETIME()) AS DATE) AS DATETIME2))),
       DATEADD(MINUTE, CASE WHEN kind = N'ORDER_CLOSED' THEN -20 ELSE -3 END, DATEADD(SECOND, hh * 3600 + mm * 60 + ss, CAST(CAST(DATEADD(DAY, -days_ago, SYSDATETIME()) AS DATE) AS DATETIME2)))
FROM @demo
WHERE order_amount IS NOT NULL;

INSERT INTO Transactions (bank_trans_id, order_id, amount, content, bank_account, gateway, status, trans_time, received_at, announced, printed)
SELECT N'FT26' + CAST(10200000 + d.n * 3917 AS NVARCHAR(12)),
       o.order_id,
       d.amount,
       COALESCE(d.content, N'TINKY' + CAST(20100 + d.n AS NVARCHAR(10))),
       N'77884268888',
       N'TPBank',
       CASE WHEN d.kind IN (N'LEGIT', N'HIGH_AMOUNT', N'BURST_RATE') THEN N'COMPLETED'
            ELSE N'CANCELLED' END,
       DATEADD(SECOND, d.hh * 3600 + d.mm * 60 + d.ss, CAST(CAST(DATEADD(DAY, -d.days_ago, SYSDATETIME()) AS DATE) AS DATETIME2)),
       DATEADD(SECOND, d.hh * 3600 + d.mm * 60 + d.ss + 6, CAST(CAST(DATEADD(DAY, -d.days_ago, SYSDATETIME()) AS DATE) AS DATETIME2)),
       1, 0
FROM @demo d
LEFT JOIN Orders o ON o.order_code = N'TINKY' + CAST(20100 + d.n AS NVARCHAR(10));

INSERT INTO FraudAlerts (trans_id, rule_code, reason, severity, status, resolved_by, resolved_notes, created_at)
SELECT t.trans_id,
       CASE d.kind WHEN N'NO_ORDER' THEN N'NO_ORDER' WHEN N'DUP_TRANS' THEN N'DUP_TRANS'
                   WHEN N'HIGH_AMOUNT' THEN N'HIGH_AMOUNT'
                   WHEN N'AMOUNT_MISMATCH' THEN N'AMOUNT_MISMATCH'
                   WHEN N'ORDER_CLOSED' THEN N'ORDER_NOT_PENDING'
                   ELSE N'BURST_RATE' END,
       CASE d.kind
            WHEN N'NO_ORDER' THEN N'No order matches the code in the transfer content.'
            WHEN N'AMOUNT_MISMATCH' THEN N'Order TINKY' + CAST(20100 + d.n AS NVARCHAR(10)) + N' expects '
                 + CAST(d.order_amount AS NVARCHAR(20)) + N' VND by transfer but ' + CAST(d.amount AS NVARCHAR(20)) + N' VND arrived.'
            WHEN N'ORDER_CLOSED' THEN N'Order TINKY' + CAST(20100 + d.n AS NVARCHAR(10)) + N' is EXPIRED and no longer waiting for payment.'
            WHEN N'DUP_TRANS' THEN N'A transaction with the same amount and content arrived within the last 60 seconds.'
            WHEN N'HIGH_AMOUNT' THEN N'Amount is 5000000 VND or more and needs a manual check.'
            ELSE N'5 or more transactions within 60 seconds.' END,
       CASE WHEN d.kind IN (N'HIGH_AMOUNT', N'BURST_RATE') THEN N'HIGH' ELSE N'MEDIUM' END,
       CASE d.n WHEN 14 THEN N'DISMISSED' WHEN 7 THEN N'RESOLVED' ELSE N'NEW' END,
       CASE WHEN d.n IN (7, 14) THEN @auditor_id ELSE NULL END,
       CASE d.n WHEN 14 THEN N'Customer sent it twice by mistake, no fraud.'
                WHEN 7 THEN N'Customer forgot the order code; confirmed by phone and handled outside the system.' ELSE NULL END,
       t.received_at
FROM @demo d
JOIN Transactions t ON t.bank_trans_id = N'FT26' + CAST(10200000 + d.n * 3917 AS NVARCHAR(12))
WHERE d.kind <> N'LEGIT';
GO

DECLARE @cashier_id INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');

INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at, created_at) VALUES
(N'TINKY84305', @cashier_id, N'nam_cashier', 2000.00, N'COMPLETED', N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=2000&addInfo=TINKY84305&accountName=LAM%20HOANG%20AN', '2026-10-12 15:00:00', '2026-10-12 14:45:00'),
(N'TINKY78040', @cashier_id, N'nam_cashier', 2000.00, N'COMPLETED', N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=2000&addInfo=TINKY78040&accountName=LAM%20HOANG%20AN', '2026-10-12 15:00:00', '2026-10-12 14:46:00'),
(N'TINKY47407', @cashier_id, N'nam_cashier', 2000.00, N'COMPLETED', N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=2000&addInfo=TINKY47407&accountName=LAM%20HOANG%20AN', '2026-10-12 15:35:00', '2026-10-12 15:20:00'),
(N'TINKY98430', @cashier_id, N'nam_cashier', 2000.00, N'COMPLETED', N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=2000&addInfo=TINKY98430&accountName=LAM%20HOANG%20AN', '2026-10-12 15:40:00', '2026-10-12 15:25:00'),
(N'TINKY27104', @cashier_id, N'nam_cashier', 2000.00, N'COMPLETED', N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=2000&addInfo=TINKY27104&accountName=LAM%20HOANG%20AN', '2026-10-12 16:45:00', '2026-10-12 16:28:00'),
(N'TINKY16282', @cashier_id, N'nam_cashier', 2000.00, N'COMPLETED', N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=2000&addInfo=TINKY16282&accountName=LAM%20HOANG%20AN', '2026-10-12 16:50:00', '2026-10-12 16:32:00');

INSERT INTO Transactions (bank_trans_id, order_id, amount, content, bank_account, gateway, status, trans_time, received_at, announced, printed) VALUES
(N'89245446', (SELECT order_id FROM Orders WHERE order_code = N'TINKY84305'), 2000.00, N'151314413497-0901675018-TINKY84305', N'77884268888', N'TPBank', N'COMPLETED', '2026-10-12 14:47:16', '2026-10-12 14:47:20', 1, 0),
(N'89245668', (SELECT order_id FROM Orders WHERE order_code = N'TINKY78040'), 2000.00, N'151314458764-0901675018-TINKY78040', N'77884268888', N'TPBank', N'COMPLETED', '2026-10-12 14:48:22', '2026-10-12 14:48:26', 1, 0),
(N'89253800', (SELECT order_id FROM Orders WHERE order_code = N'TINKY47407'), 2000.00, N'151318377405-0901675018-TINKY47407', N'77884268888', N'TPBank', N'COMPLETED', '2026-10-12 15:23:36', '2026-10-12 15:23:40', 1, 0),
(N'89254760', (SELECT order_id FROM Orders WHERE order_code = N'TINKY98430'), 2000.00, N'151318890448 0901675018 TINKY98430', N'77884268888', N'TPBank', N'COMPLETED', '2026-10-12 15:27:21', '2026-10-12 15:27:25', 1, 0),
(N'89273074', (SELECT order_id FROM Orders WHERE order_code = N'TINKY27104'), 2000.00, N'TINKY27104', N'77884268888', N'TPBank', N'COMPLETED', '2026-10-12 16:30:21', '2026-10-12 16:30:25', 1, 0),
(N'89274782', (SELECT order_id FROM Orders WHERE order_code = N'TINKY16282'), 2000.00, N'151327617041 0901675018 TINKY16282', N'77884268888', N'TPBank', N'COMPLETED', '2026-10-12 16:35:04', '2026-10-12 16:35:08', 1, 0);
GO

DECLARE @events TABLE (seq INT IDENTITY(1,1), actor NVARCHAR(50), action NVARCHAR(50), shift_id NVARCHAR(50), detail NVARCHAR(200), event_time DATETIME2(3));
DECLARE @day INT = 6;
WHILE @day >= 0
BEGIN
    DECLARE @base DATETIME2(3) = CAST(CAST(DATEADD(DAY, -@day, SYSDATETIME()) AS DATE) AS DATETIME2(3));
    DECLARE @sid NVARCHAR(50) = N'S' + CONVERT(NVARCHAR(8), @base, 112) + N'-070000';
    INSERT INTO @events (actor, action, shift_id, detail, event_time) VALUES (N'nam_cashier', N'OPEN_SHIFT', @sid, N'{}', DATEADD(HOUR, 7, @base));
    IF @day > 0
        INSERT INTO @events (actor, action, shift_id, detail, event_time) VALUES
        (CASE WHEN @day = 4 THEN N'han_admin' ELSE N'nam_cashier' END, N'CLOSE_SHIFT', @sid,
         CASE WHEN @day = 4 THEN N'{"forced":"true"}' ELSE N'{"counted_cash":"0"}' END, DATEADD(HOUR, 21, @base));
    SET @day = @day - 1;
END

INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, created_at)
SELECT actor, action, N'SHIFT', shift_id, detail, event_time FROM @events ORDER BY seq;
GO

IF NOT EXISTS (SELECT 1 FROM Commissions)
BEGIN
    DECLARE @camp1 INT = (SELECT campaign_id FROM Campaigns WHERE name = N'Autumn Push');
    DECLARE @camp2 INT = (SELECT campaign_id FROM Campaigns WHERE name = N'Weekend Fixed Bonus');
    DECLARE @pick TABLE (rn INT, order_id INT, amount DECIMAL(18,2), code NVARCHAR(30), camp INT, pct DECIMAL(5,2), fixed_amount DECIMAL(18,2), st NVARCHAR(12), age INT, reason NVARCHAR(200));
    INSERT INTO @pick (rn, order_id, amount, code, camp, pct, fixed_amount, st, age, reason)
    SELECT p.rn, o.order_id, o.amount, p.code, p.camp, p.pct, p.fixed_amount, p.st, p.age, p.reason
    FROM (SELECT order_id, amount, ROW_NUMBER() OVER (ORDER BY order_id) AS rn FROM Orders WHERE status IN (N'COMPLETED', N'PAID')) o
    JOIN (VALUES
    (1, N'LINH08', @camp1, 10.00, 0.00, N'PAID', 40, NULL),
    (2, N'LINH08', @camp1, 10.00, 0.00, N'PAID', 36, NULL),
    (3, N'MINH05', @camp1, 10.00, 0.00, N'APPROVED', 9, NULL),
    (4, N'HANA10', @camp1, 10.00, 0.00, N'PENDING', 1, NULL),
    (5, N'HANA10', @camp1, 10.00, 0.00, N'AVAILABLE', 20, NULL),
    (6, N'MINH05', @camp2, 0.00, 10000.00, N'PENDING', 2, NULL),
    (7, N'QUAN99', @camp1, 10.00, 0.00, N'ON_HOLD', 26, N'Held for review: repeated orders from the same customer.'),
    (8, N'LINH08', @camp1, 10.00, 0.00, N'AVAILABLE', 25, NULL),
    (9, N'LINH08', @camp1, 10.00, 0.00, N'AVAILABLE', 24, NULL),
    (11, N'LINH08', @camp1, 10.00, 0.00, N'AVAILABLE', 23, NULL),
    (10, N'MINH05', @camp2, 0.00, 10000.00, N'AVAILABLE', 21, NULL),
    (12, N'MINH05', @camp2, 0.00, 10000.00, N'AVAILABLE', 19, NULL),
    (14, N'MINH05', @camp2, 0.00, 10000.00, N'AVAILABLE', 18, NULL),
    (15, N'MINH05', @camp2, 0.00, 10000.00, N'AVAILABLE', 17, NULL),
    (16, N'MINH05', @camp2, 0.00, 10000.00, N'AVAILABLE', 16, NULL)
    ) AS p(rn, code, camp, pct, fixed_amount, st, age, reason) ON p.rn = o.rn;

    UPDATE o SET referred_by_affiliate_id = a.affiliate_id, referral_campaign_id = k.camp
    FROM Orders o JOIN @pick k ON k.order_id = o.order_id JOIN Affiliates a ON a.referral_code = k.code;

    INSERT INTO Commissions (affiliate_id, order_id, campaign_id, order_amount, eligible_amount, rate_text, original_amount, amount, status, rule_snapshot, reason, created_at, updated_at)
    SELECT a.affiliate_id, k.order_id, k.camp, k.amount, k.amount,
           CASE WHEN k.pct > 0 THEN CAST(CAST(k.pct AS DECIMAL(5,1)) AS NVARCHAR(10)) + N'%' ELSE N'10,000 VND per order' END,
           CASE WHEN k.pct > 0 THEN ROUND(k.amount * k.pct / 100, 0) ELSE CASE WHEN k.fixed_amount > k.amount THEN k.amount ELSE k.fixed_amount END END,
           CASE WHEN k.pct > 0 THEN ROUND(k.amount * k.pct / 100, 0) ELSE CASE WHEN k.fixed_amount > k.amount THEN k.amount ELSE k.fixed_amount END END,
           k.st,
           (SELECT name FROM Campaigns WHERE campaign_id = k.camp) + N' | ' + CASE WHEN k.pct > 0 THEN CAST(CAST(k.pct AS DECIMAL(5,1)) AS NVARCHAR(10)) + N'% of eligible amount' ELSE N'10,000 VND per order' END,
           k.reason, DATEADD(DAY, -k.age, SYSDATETIME()), DATEADD(DAY, -k.age, SYSDATETIME())
    FROM @pick k JOIN Affiliates a ON a.referral_code = k.code;

    DECLARE @aff_linh INT = (SELECT affiliate_id FROM Affiliates WHERE referral_code = N'LINH08');
    DECLARE @gross DECIMAL(18,2) = (SELECT SUM(amount) FROM Commissions WHERE affiliate_id = @aff_linh AND status = N'PAID');
    INSERT INTO Payouts (affiliate_id, batch_code, gross, deduction, net, status, bank_info, reference, requested_at, paid_at, paid_by)
    VALUES (@aff_linh, N'BATCH-01', @gross, CASE WHEN @gross >= 60000 THEN ROUND(@gross * 0.1, 0) ELSE 0 END,
            @gross - CASE WHEN @gross >= 60000 THEN ROUND(@gross * 0.1, 0) ELSE 0 END, N'PAID', N'Demo bank 0071 0012 3456',
            N'FT26DEMO0001', DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY, -29, SYSDATETIME()), (SELECT user_id FROM Users WHERE user_code = N'han_admin'));
    UPDATE Commissions SET payout_id = SCOPE_IDENTITY() WHERE affiliate_id = @aff_linh AND status = N'PAID';
END
GO

IF NOT EXISTS (SELECT 1 FROM TaxObligations)
BEGIN
    DECLARE @admin INT = (SELECT user_id FROM Users WHERE user_code = N'han_admin');
    DECLARE @mgr INT = (SELECT user_id FROM Users WHERE user_code = N'mai_manager');
    DECLARE @cur DATE = DATEFROMPARTS(YEAR(GETDATE()), (DATEPART(QUARTER, GETDATE()) - 1) * 3 + 1, 1);
    DECLARE @prev DATE = DATEADD(QUARTER, -1, @cur);
    DECLARE @prevLabel NVARCHAR(40) = N'Q' + CAST(DATEPART(QUARTER, @prev) AS NVARCHAR(1)) + N' ' + CAST(YEAR(@prev) AS NVARCHAR(4));
    DECLARE @curLabel NVARCHAR(40) = N'Q' + CAST(DATEPART(QUARTER, @cur) AS NVARCHAR(1)) + N' ' + CAST(YEAR(@cur) AS NVARCHAR(4));
    INSERT INTO TaxObligations (kind, period_label, due_date, status, responsible_user, estimated_amount, declared_amount, confirmed_amount, submission_ref, notes)
    VALUES
    (N'VAT', @prevLabel, EOMONTH(DATEADD(MONTH, 3, @prev)), N'AWAITING_PAYMENT', @mgr, 1200000, 1200000, 1200000, N'DECL-DEMO-001', N'Declared, payment in progress.'),
    (N'PIT', @prevLabel, EOMONTH(DATEADD(MONTH, 3, @prev)), N'SUBMITTED', @mgr, 450000, 450000, NULL, N'DECL-DEMO-002', N'Waiting for the confirmed amount.'),
    (N'VAT', @curLabel, EOMONTH(DATEADD(MONTH, 3, @cur)), N'PREPARATION', @mgr, 900000, NULL, NULL, NULL, NULL),
    (N'EINVOICE', N'Year ' + CAST(YEAR(GETDATE()) AS NVARCHAR(4)), DATEADD(DAY, 14, CAST(GETDATE() AS DATE)), N'NEEDS_INFO', @admin, 0, NULL, NULL, NULL, N'Check the e-invoice provider registration.'),
    (N'BOOKKEEPING', N'Month ' + FORMAT(GETDATE(), 'yyyy-MM'), EOMONTH(GETDATE()), N'APPLICABLE', @mgr, 0, NULL, NULL, NULL, NULL);

    INSERT INTO TaxObligationHistory (obligation_id, from_status, to_status, note, changed_by)
    SELECT obligation_id, N'SUBMITTED', N'AWAITING_PAYMENT', N'Amount confirmed by the tax office notice.', @mgr FROM TaxObligations WHERE kind = N'VAT' AND status = N'AWAITING_PAYMENT';

    INSERT INTO TaxPayments (obligation_id, paid_on, amount, method, reference, receipt_name, recorded_by)
    SELECT obligation_id, DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), 500000, N'Bank transfer', N'FT26DEMO5001', N'receipt-demo.jpg', @mgr
    FROM TaxObligations WHERE kind = N'VAT' AND status = N'AWAITING_PAYMENT';
END
GO

IF NOT EXISTS (SELECT 1 FROM Expenses)
BEGIN
    DECLARE @uid INT = (SELECT user_id FROM Users WHERE user_code = N'mai_manager');
    INSERT INTO Expenses (spent_on, supplier, supplier_tax_code, category, amount, payment_method, invoice_no, has_document, tax_treatment, created_by)
    VALUES
    (DATEADD(DAY, -2, CAST(GETDATE() AS DATE)), N'Hat Viet Coffee', N'0312345601', N'Ingredients', 4200000, N'Bank transfer', N'INV-1001', 1, N'MAY_COUNT', @uid),
    (DATEADD(DAY, -6, CAST(GETDATE() AS DATE)), N'Green Milk Co', N'0301234502', N'Ingredients', 2650000, N'Bank transfer', N'INV-1002', 1, N'UNCLASSIFIED', @uid),
    (DATEADD(DAY, -9, CAST(GETDATE() AS DATE)), N'Minh Phat Packaging', N'0309988703', N'Packaging', 1350000, N'Cash', NULL, 0, N'UNCLASSIFIED', @uid),
    (DATEADD(DAY, -12, CAST(GETDATE() AS DATE)), N'Landlord', NULL, N'Rent', 18000000, N'Bank transfer', NULL, 1, N'UNCLASSIFIED', @uid),
    (DATEADD(DAY, -15, CAST(GETDATE() AS DATE)), N'City Power (sample)', N'0100100104', N'Utilities', 2300000, N'Bank transfer', N'INV-1003', 1, N'MAY_COUNT', @uid),
    (DATEADD(DAY, -20, CAST(GETDATE() AS DATE)), N'Social Ads', NULL, N'Marketing', 1500000, N'Card', NULL, 0, N'UNCLASSIFIED', @uid),
    (DATEADD(DAY, -27, CAST(GETDATE() AS DATE)), N'Hat Viet Coffee', N'0312345601', N'Ingredients', 3900000, N'Bank transfer', N'INV-0998', 1, N'UNCLASSIFIED', @uid),
    (DATEADD(DAY, -35, CAST(GETDATE() AS DATE)), N'Kitchen Tools Store', NULL, N'Equipment', 5200000, N'Cash', NULL, 0, N'NOT_COUNTED', @uid);
END
GO

IF NOT EXISTS (SELECT 1 FROM TaxDocuments)
BEGIN
    INSERT INTO TaxDocuments (doc_name, category, period_label, status)
    VALUES
    (N'Business registration certificate', N'REGISTRATION', NULL, N'MISSING'),
    (N'Tax code certificate', N'TAX_CODE', NULL, N'MISSING'),
    (N'Latest bank statement', N'BANK', NULL, N'MISSING'),
    (N'Premises rental contract', N'OTHER', NULL, N'MISSING');
END
GO

IF NOT EXISTS (SELECT 1 FROM ChannelCommissions)
BEGIN
    DECLARE @mgr INT = (SELECT user_id FROM Users WHERE user_code = N'mai_manager');
    DECLARE @adm INT = (SELECT user_id FROM Users WHERE user_code = N'han_admin');
    DECLARE @linh INT = (SELECT affiliate_id FROM Affiliates WHERE name = N'Linh Review');
    DECLARE @minh INT = (SELECT affiliate_id FROM Affiliates WHERE name = N'Minh Unbox');
    DECLARE @pay INT;
    INSERT INTO ChannelPayouts (reference, affiliate_id, total_amount, payment_method, payment_reference, payment_date, status, created_by, confirmed_by, confirmed_at)
    VALUES (N'CP-0001', @minh, 15000, N'BANK_TRANSFER', N'FT26100100771', DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), N'CONFIRMED', @mgr, @adm, DATEADD(DAY, -5, SYSDATETIME()));
    SET @pay = SCOPE_IDENTITY();
    INSERT INTO ChannelCommissions (affiliate_id, channel, external_ref, platform_order_id, order_date, base_amount, commission_type, commission_rate,
        calculated_amount, reported_amount, approved_amount, status, data_source, attribution, reject_reason, payout_id, created_by, approved_by, approved_at)
    VALUES
    (@linh, N'SHOPEE', N'SPC-1001', N'SPC-1001', DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), 400000, N'PERCENT', 8, 32000, 32000, NULL, N'PENDING', N'CSV_IMPORT', N'LINH08', NULL, NULL, @mgr, NULL, NULL),
    (@linh, N'SHOPEE', N'SPC-1002', N'SPC-1002', DATEADD(DAY, -8, CAST(GETDATE() AS DATE)), 250000, N'PERCENT', 8, 20000, 20000, 20000, N'APPROVED', N'CSV_IMPORT', N'LINH08', NULL, NULL, @mgr, @mgr, DATEADD(DAY, -6, SYSDATETIME())),
    (@minh, N'TIKTOK', N'TTC-2001', N'TTC-2001', DATEADD(DAY, -9, CAST(GETDATE() AS DATE)), 600000, N'PERCENT', 5, 30000, 30000, 30000, N'APPROVED', N'CSV_IMPORT', N'MINH05', NULL, NULL, @mgr, @mgr, DATEADD(DAY, -6, SYSDATETIME())),
    (@minh, N'TIKTOK', N'TTC-2002', N'TTC-2002', DATEADD(DAY, -10, CAST(GETDATE() AS DATE)), 180000, N'PERCENT', 5, 9000, 9000, NULL, N'REJECTED', N'CSV_IMPORT', N'MINH05', N'Order returned by the buyer', NULL, @mgr, @mgr, DATEADD(DAY, -7, SYSDATETIME())),
    (@linh, N'SHOPEE', N'SPC-1003', N'SPC-1003', DATEADD(DAY, -2, CAST(GETDATE() AS DATE)), 500000, N'PERCENT', 8, 40000, NULL, NULL, N'PENDING', N'MANUAL', N'Typed by the store', NULL, NULL, @mgr, NULL, NULL),
    (@minh, N'TIKTOK', N'TTC-2003', N'TTC-2003', DATEADD(DAY, -14, CAST(GETDATE() AS DATE)), 300000, N'PERCENT', 5, 15000, 15000, 15000, N'PAID', N'CSV_IMPORT', N'MINH05', NULL, @pay, @mgr, @mgr, DATEADD(DAY, -10, SYSDATETIME()));
    INSERT INTO ChannelPayoutItems (payout_id, commission_id, amount)
    SELECT @pay, commission_id, 15000 FROM ChannelCommissions WHERE external_ref = N'TTC-2003';
END
GO

PRINT N'>>> Database and sample data for PaymentSpeaker loaded successfully!';
GO
SET NOEXEC OFF;
GO

IF NOT EXISTS (SELECT 1 FROM StockMovements WHERE reason IN (N'RESTOCK', N'ADJUST', N'SALE'))
    UPDATE Products SET stock_quantity = CASE product_id % 10 WHEN 0 THEN 0 WHEN 1 THEN 3 WHEN 2 THEN 5 ELSE 15 + (product_id * 7) % 40 END
    WHERE stock_quantity = 0
      AND NOT EXISTS (SELECT 1 FROM StockMovements m WHERE m.product_id = Products.product_id);
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT N'stock_low_threshold', N'5'
WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = N'stock_low_threshold');
GO

INSERT INTO StockMovements (product_id, quantity_change, quantity_after, reason, note)
SELECT p.product_id, p.stock_quantity, p.stock_quantity, N'INITIAL', N'Opening balance'
FROM Products p
WHERE p.stock_quantity <> 0
  AND NOT EXISTS (SELECT 1 FROM StockMovements m WHERE m.product_id = p.product_id);
GO

IF NOT EXISTS (SELECT 1 FROM StockMovements WHERE reason IN (N'RESTOCK', N'ADJUST', N'SALE'))
BEGIN
    DECLARE @mgr_id INT = (SELECT user_id FROM Users WHERE user_code = N'mai_manager');
    DECLARE @sale_order INT = (SELECT TOP 1 order_id FROM Orders WHERE status = N'COMPLETED' ORDER BY order_id);
    DECLARE @pick TABLE (rn INT, product_id INT);
    INSERT INTO @pick (rn, product_id)
    SELECT ROW_NUMBER() OVER (ORDER BY product_id), product_id
    FROM Products WHERE product_code NOT LIKE N'DEMO%';
    DECLARE @mv TABLE (seq INT IDENTITY(1,1), rn INT, reason NVARCHAR(10), change INT, note NVARCHAR(200), days_ago INT);
    INSERT INTO @mv (rn, reason, change, note, days_ago) VALUES
    (1, N'RESTOCK', 24, N'Weekly delivery from supplier', 6),
    (1, N'SALE', -3, N'Sold at the counter', 4),
    (2, N'RESTOCK', 12, N'Restocked after a sell-out', 5),
    (2, N'ADJUST', -2, N'Damaged items found during the count', 3),
    (3, N'ADJUST', 4, N'Counted more than the system showed', 2),
    (4, N'RESTOCK', 30, N'Monthly bulk order', 1),
    (4, N'SALE', -6, N'Sold at the counter', 1);
    DECLARE @s INT = 1;
    DECLARE @max INT = (SELECT MAX(seq) FROM @mv);
    DECLARE @pid INT, @chg INT, @after INT, @why NVARCHAR(10), @memo NVARCHAR(200), @ago INT;
    WHILE @s <= @max
    BEGIN
        SET @pid = NULL;
        SELECT @pid = p.product_id, @chg = m.change, @why = m.reason, @memo = m.note, @ago = m.days_ago
        FROM @mv m JOIN @pick p ON p.rn = m.rn WHERE m.seq = @s;
        IF @pid IS NOT NULL
        BEGIN
            UPDATE Products SET stock_quantity = stock_quantity + @chg WHERE product_id = @pid;
            SET @after = (SELECT stock_quantity FROM Products WHERE product_id = @pid);
            INSERT INTO StockMovements (product_id, quantity_change, quantity_after, reason, order_id, note, created_by, created_at)
            VALUES (@pid, @chg, @after, @why, CASE WHEN @why = N'SALE' THEN @sale_order ELSE NULL END, @memo,
                    CASE WHEN @why = N'SALE' THEN NULL ELSE @mgr_id END, DATEADD(DAY, -@ago, SYSDATETIME()));
        END
        SET @s = @s + 1;
    END
END
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT v.k, v.v FROM (VALUES
    (N'feature_tax', N'1'), (N'feature_online', N'1'), (N'feature_affiliate', N'1'), (N'feature_ai', N'1'),
    (N'setup_done', N'1'),
    (N'taxp_name', N'Tinky Cafe'), (N'taxp_type', N'HOUSEHOLD'), (N'taxp_address', N'District 1, Ho Chi Minh City'),
    (N'taxp_branches', N'1'), (N'taxp_channels', N'BOTH'), (N'taxp_registration', N'REGISTERED'),
    (N'taxp_tin', N'0312345678'), (N'taxp_method', N'QUARTERLY'), (N'taxp_einvoice', N'NO')
) AS v(k, v)
WHERE NOT EXISTS (SELECT 1 FROM Settings s WHERE s.setting_key = v.k);
GO

IF NOT EXISTS (SELECT 1 FROM Transactions WHERE bank_trans_id = N'FT26RV0001')
BEGIN
    DECLARE @cash_id INT = (SELECT user_id FROM Users WHERE user_code = N'nam_cashier');
    DECLARE @boss_id INT = (SELECT user_id FROM Users WHERE user_code = N'mai_manager');
    INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at, created_at)
    VALUES (N'TINKY30001', @cash_id, N'nam_cashier', 45000.00, N'COMPLETED',
            N'https://img.vietqr.io/image/TPBank-77884268888-compact2.png?amount=45000&addInfo=TINKY30001&accountName=LAM%20HOANG%20AN',
            DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -3, SYSDATETIME()));
    DECLARE @linked INT = SCOPE_IDENTITY();
    INSERT INTO Transactions (bank_trans_id, order_id, amount, content, bank_account, gateway, status, trans_time, received_at, announced, printed,
                              resolution, close_category, resolution_note, resolved_by, resolved_at, linked_order_id)
    VALUES
    (N'FT26RV0001', NULL, 120000.00, N'Customer forgot the order code', N'77884268888', N'TPBank', N'CANCELLED',
     DATEADD(MINUTE, -40, SYSDATETIME()), DATEADD(MINUTE, -40, SYSDATETIME()), 1, 0, NULL, NULL, NULL, NULL, NULL, NULL),
    (N'FT26RV0002', NULL, 30000.00, N'Wrong account transfer', N'77884268888', N'TPBank', N'CANCELLED',
     DATEADD(HOUR, -5, SYSDATETIME()), DATEADD(HOUR, -5, SYSDATETIME()), 1, 0, N'CLOSED', N'WRONG_ACCOUNT_OR_SPAM',
     N'Sender confirmed the transfer was meant for another shop', @boss_id, DATEADD(HOUR, -4, SYSDATETIME()), NULL),
    (N'FT26RV0003', @linked, 45000.00, N'Coffee payment without code', N'77884268888', N'TPBank', N'COMPLETED',
     DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -3, SYSDATETIME()), 1, 1, N'LINKED', NULL,
     N'Matched to the order by phone call with the customer', @boss_id, DATEADD(HOUR, -2, SYSDATETIME()), @linked);
    INSERT INTO FraudAlerts (trans_id, rule_code, reason, severity, status, resolved_by, resolved_notes, created_at)
    SELECT trans_id, N'NO_ORDER', N'No order matches the code in the transfer content.', N'MEDIUM',
           CASE bank_trans_id WHEN N'FT26RV0001' THEN N'NEW' ELSE N'RESOLVED' END,
           CASE bank_trans_id WHEN N'FT26RV0001' THEN NULL ELSE @boss_id END,
           CASE bank_trans_id WHEN N'FT26RV0001' THEN NULL ELSE resolution_note END,
           received_at
    FROM Transactions WHERE bank_trans_id IN (N'FT26RV0001', N'FT26RV0002', N'FT26RV0003');
    INSERT INTO OrderItems (order_id, product_id, product_name, unit_price, quantity, subtotal)
    VALUES (@linked, NULL, N'Quick Custom Sale', 45000.00, 1, 45000.00);
END
GO

IF NOT EXISTS (SELECT 1 FROM Invoices)
BEGIN
    DECLARE @inv_user INT = (SELECT user_id FROM Users WHERE user_code = N'han_admin');
    DECLARE @inv_orders TABLE (rn INT, order_id INT, order_code NVARCHAR(20), amount DECIMAL(18,2));
    INSERT INTO @inv_orders (rn, order_id, order_code, amount)
    SELECT ROW_NUMBER() OVER (ORDER BY order_id), order_id, order_code, amount
    FROM Orders WHERE order_code LIKE N'TINKY201__' AND status = N'COMPLETED';
    INSERT INTO Invoices (invoice_no, source_type, source_id, buyer_name, buyer_tax_code, buyer_address, total_amount, vat_amount, status,
                          provider_invoice_no, provider_lookup_code, created_by, created_at)
    SELECT N'TMP' + CAST(o.rn AS NVARCHAR(5)), N'ORDER', o.order_id,
           CASE o.rn WHEN 1 THEN N'Walk-in customer' WHEN 2 THEN N'Sunrise Trading Co' WHEN 3 THEN N'Walk-in customer' ELSE N'Green Leaf Ltd' END,
           CASE WHEN o.rn IN (2, 4) THEN N'0312345001' ELSE NULL END, NULL,
           CASE WHEN o.rn = 4 THEN o.amount + 5000 ELSE o.amount END,
           ROUND(o.amount * 8 / 108, 0),
           CASE o.rn WHEN 3 THEN N'SENT_TO_PROVIDER' ELSE N'ISSUED' END,
           CASE WHEN o.rn IN (1, 2, 4) THEN N'C26TAA' + CAST(1000 + o.rn AS NVARCHAR(6)) ELSE NULL END,
           CASE WHEN o.rn IN (1, 2, 4) THEN N'LK26' + CAST(5000 + o.rn AS NVARCHAR(6)) ELSE NULL END,
           @inv_user, DATEADD(DAY, -o.rn, SYSDATETIME())
    FROM @inv_orders o WHERE o.rn <= 4;
    UPDATE Invoices SET invoice_no = N'INV' + RIGHT(N'000000' + CAST(invoice_id AS NVARCHAR(10)), 6) WHERE invoice_no LIKE N'TMP%';
    INSERT INTO InvoiceLines (invoice_id, name, quantity, unit_price, subtotal)
    SELECT i.invoice_id, N'Order ' + o.order_code, 1, i.total_amount, i.total_amount
    FROM Invoices i JOIN Orders o ON o.order_id = i.source_id WHERE i.source_type = N'ORDER';
    INSERT INTO Invoices (invoice_no, source_type, source_id, buyer_name, total_amount, vat_amount, status, created_by)
    SELECT TOP 1 N'TMPCH', N'CHANNEL_SALE', sale_id, N'Online buyer', goods_amount, ROUND(goods_amount * 8 / 108, 0), N'DRAFT', @inv_user
    FROM ChannelSales WHERE status = N'COMPLETED' ORDER BY sale_id;
    UPDATE Invoices SET invoice_no = N'INV' + RIGHT(N'000000' + CAST(invoice_id AS NVARCHAR(10)), 6) WHERE invoice_no = N'TMPCH';
    INSERT INTO InvoiceLines (invoice_id, name, quantity, unit_price, subtotal)
    SELECT i.invoice_id, N'Online order ' + s.external_ref, 1, i.total_amount, i.total_amount
    FROM Invoices i JOIN ChannelSales s ON s.sale_id = i.source_id WHERE i.source_type = N'CHANNEL_SALE';
END
GO

INSERT INTO TaxReconNotes (ref_key, note, resolved, updated_by)
SELECT N'ORDER:' + CAST(o.order_id AS NVARCHAR(10)), N'Customer did not ask for an invoice; keep the receipt only.', 1,
       (SELECT user_id FROM Users WHERE user_code = N'han_admin')
FROM (SELECT TOP 1 order_id FROM Orders
      WHERE status = N'COMPLETED' AND order_id NOT IN (SELECT source_id FROM Invoices WHERE source_type = N'ORDER')
      ORDER BY order_id DESC) o
WHERE NOT EXISTS (SELECT 1 FROM TaxReconNotes);
GO

INSERT INTO TaxAssessments (assessed_on, period_label, summary, verification, created_by)
SELECT CAST(GETDATE() AS DATE), N'Year to date',
       N'VAT: needs verification. PIT: needs verification. E-invoice: needs verification. Bookkeeping: applicable.',
       N'NEEDS_VERIFICATION', (SELECT user_id FROM Users WHERE user_code = N'han_admin')
WHERE NOT EXISTS (SELECT 1 FROM TaxAssessments);
GO

INSERT INTO TaxCalculations (calc_date, period_label, period_from, period_to, sector, revenue, adjustment, vat_amount, pit_amount, total_amount,
                             rule_stamp, verified_rules, note, created_by)
SELECT CAST(GETDATE() AS DATE), N'Demo period', DATEFROMPARTS(YEAR(GETDATE()), 1, 1), CAST(GETDATE() AS DATE), N'GOODS',
       100000000, 0, 1000000, 0, 1000000, N'Demo rules from the seed data', 0, N'Demo calculation, not the amount to pay',
       (SELECT user_id FROM Users WHERE user_code = N'han_admin')
WHERE NOT EXISTS (SELECT 1 FROM TaxCalculations);
GO

INSERT INTO ChannelSaleItems (sale_id, sku_code, product_name, variation_name, quantity, unit_price, subtotal, cost_price)
SELECT s.sale_id, N'SKU-' + RIGHT(N'0000' + CAST(s.sale_id AS NVARCHAR(10)), 4),
       CASE s.sale_id % 3 WHEN 0 THEN N'Cold brew bottle' WHEN 1 THEN N'Drip coffee bag' ELSE N'Coffee beans 250g' END,
       NULL, 1, s.goods_amount, s.goods_amount, s.cost_amount
FROM ChannelSales s
WHERE NOT EXISTS (SELECT 1 FROM ChannelSaleItems);
GO

INSERT INTO CommissionAdjustments (commission_id, affiliate_id, amount, reason, payout_id, created_by)
SELECT TOP 1 c.commission_id, c.affiliate_id, -5000, N'Customer returned part of the order', NULL,
       (SELECT user_id FROM Users WHERE user_code = N'mai_manager')
FROM Commissions c
WHERE c.status = N'PAID' AND NOT EXISTS (SELECT 1 FROM CommissionAdjustments);
GO

INSERT INTO GuideProgress (user_id, article_id, kind, state)
SELECT u.user_id, v.article_id, v.kind, v.state
FROM (VALUES
    (N'han_admin', N'preparation', N'READ', N'CHECKED'),
    (N'han_admin', N'legal-tax', N'BOOKMARK', N'SAVED'),
    (N'han_admin', N'register-hkd-2026', N'READ', N'CHECKED'),
    (N'nam_cashier', N'pos-operations', N'READ', N'CHECKED'),
    (N'nam_cashier', N'pos-operations', N'BOOKMARK', N'SAVED')
) AS v(user_code, article_id, kind, state)
JOIN Users u ON u.user_code = v.user_code
WHERE NOT EXISTS (SELECT 1 FROM GuideProgress g WHERE g.user_id = u.user_id AND g.article_id = v.article_id AND g.kind = v.kind);
GO

INSERT INTO Stores (store_code, store_name, address)
SELECT N'TINKY-HCM', N'Tinky Cafe', N'District 1, Ho Chi Minh City'
WHERE NOT EXISTS (SELECT 1 FROM Stores);
GO

DECLARE @sid INT = (SELECT TOP 1 store_id FROM Stores ORDER BY store_id);
DECLARE @admin_role INT = (SELECT role_id FROM Roles WHERE role_name = N'ADMIN');
UPDATE Users SET store_id = @sid WHERE store_id IS NULL;
UPDATE Devices SET store_id = @sid WHERE store_id IS NULL;
UPDATE Categories SET store_id = @sid WHERE store_id IS NULL;
UPDATE DiscountCodes SET store_id = @sid WHERE store_id IS NULL;
UPDATE Orders SET store_id = @sid WHERE store_id IS NULL;
UPDATE Transactions SET store_id = @sid WHERE store_id IS NULL;
UPDATE Affiliates SET store_id = @sid WHERE store_id IS NULL;
UPDATE Campaigns SET store_id = @sid WHERE store_id IS NULL;
UPDATE ChannelSales SET store_id = @sid WHERE store_id IS NULL;
UPDATE Invoices SET store_id = @sid WHERE store_id IS NULL;
UPDATE Expenses SET store_id = @sid WHERE store_id IS NULL;
UPDATE TaxObligations SET store_id = @sid WHERE store_id IS NULL;
UPDATE TaxDocuments SET store_id = @sid WHERE store_id IS NULL;
UPDATE TaxAssessments SET store_id = @sid WHERE store_id IS NULL;
UPDATE TaxCalculations SET store_id = @sid WHERE store_id IS NULL;
UPDATE ChannelPayouts SET store_id = @sid WHERE store_id IS NULL;
UPDATE Payouts SET store_id = @sid WHERE store_id IS NULL;
UPDATE [Groups] SET store_id = @sid WHERE store_id IS NULL;
UPDATE Products SET store_id = @sid WHERE store_id IS NULL;
UPDATE AuditLogs SET store_id = @sid
WHERE store_id IS NULL
  AND actor_username NOT IN (SELECT user_code FROM Users WHERE role_id = @admin_role);
UPDATE Settings SET store_id = @sid
WHERE store_id = 0 AND setting_key NOT LIKE N'sepay_last_%' AND setting_key <> N'setup_done';
GO

EXEC dbo.usp_AuditChainBackfill;
GO
