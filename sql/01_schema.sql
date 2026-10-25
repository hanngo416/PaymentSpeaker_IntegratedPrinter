
USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'PaymentSpeaker')
BEGIN
    CREATE DATABASE PaymentSpeaker;
    PRINT N'>>> Database PaymentSpeaker created successfully!';
END
GO

USE PaymentSpeaker;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

GO

IF OBJECT_ID('Roles', 'U') IS NULL
CREATE TABLE Roles (
    role_id INT PRIMARY KEY,
    role_name NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL
);
GO

IF OBJECT_ID('Users', 'U') IS NULL
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    user_code NVARCHAR(50) NOT NULL UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(256) NOT NULL UNIQUE,
    google_id NVARCHAR(64) NULL,
    role_id INT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Users_google_id' AND object_id = OBJECT_ID('Users'))
CREATE UNIQUE INDEX UX_Users_google_id ON Users(google_id) WHERE google_id IS NOT NULL;
GO

IF OBJECT_ID('LoginOtps', 'U') IS NULL
CREATE TABLE LoginOtps (
    otp_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    otp_hash NVARCHAR(256) NOT NULL,
    salt NVARCHAR(64) NOT NULL,
    expires_at DATETIME2(3) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_LoginOtps_Users FOREIGN KEY (user_id) REFERENCES Users(user_id)
);
GO

IF OBJECT_ID('Devices', 'U') IS NULL
CREATE TABLE Devices (
    device_id INT IDENTITY(1,1) PRIMARY KEY,
    device_code NVARCHAR(50) NOT NULL UNIQUE,
    device_type NVARCHAR(20) NOT NULL,
    model_name NVARCHAR(100) NOT NULL,
    api_key NVARCHAR(100) NOT NULL UNIQUE,
    status NVARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    last_ping DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Device_Type CHECK (device_type IN ('PRINTER'))
);
GO

IF OBJECT_ID('MaintenanceTickets', 'U') IS NOT NULL DROP TABLE MaintenanceTickets;
GO

IF OBJECT_ID('Categories', 'U') IS NULL
CREATE TABLE Categories (
    category_id INT IDENTITY(1,1) PRIMARY KEY,
    category_name NVARCHAR(50) NOT NULL UNIQUE,
    display_order INT NOT NULL DEFAULT 0
);
GO

IF OBJECT_ID('Groups', 'U') IS NULL
CREATE TABLE [Groups] (
    group_id INT IDENTITY(1,1) PRIMARY KEY,
    category_id INT NOT NULL,
    group_name NVARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT FK_Groups_Categories FOREIGN KEY (category_id) REFERENCES Categories(category_id),
    CONSTRAINT UQ_Category_Group UNIQUE (category_id, group_name)
);
GO

IF OBJECT_ID('Products', 'U') IS NULL
CREATE TABLE Products (
    product_id INT IDENTITY(1,1) PRIMARY KEY,
    group_id INT NOT NULL,
    product_code NVARCHAR(30) NOT NULL UNIQUE,
    product_name NVARCHAR(100) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT FK_Products_Groups FOREIGN KEY (group_id) REFERENCES [Groups](group_id),
    CONSTRAINT CHK_Product_Price CHECK (price > 0)
);
GO

IF OBJECT_ID('Orders', 'U') IS NULL
CREATE TABLE Orders (
    order_id INT IDENTITY(1,1) PRIMARY KEY,
    order_code NVARCHAR(50) NOT NULL UNIQUE,
    created_by INT NOT NULL,
    created_by_code NVARCHAR(50) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    cash_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    qr_content NVARCHAR(500) NULL,
    expires_at DATETIME2(3) NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Orders_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Order_Status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'PAID'))
);
GO

IF COL_LENGTH('Orders', 'cash_amount') IS NULL
BEGIN
    ALTER TABLE Orders ADD cash_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_cash_amount DEFAULT 0;
END
GO

IF COL_LENGTH('Orders', 'paid_amount') IS NOT NULL
BEGIN
    DECLARE @df NVARCHAR(200) = (SELECT dc.name FROM sys.default_constraints dc
        JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID('Orders') AND c.name = 'paid_amount');
    IF @df IS NOT NULL EXEC(N'ALTER TABLE Orders DROP CONSTRAINT ' + @df);
    ALTER TABLE Orders DROP COLUMN paid_amount;
END
GO

IF OBJECT_ID('DiscountCodes', 'U') IS NULL
CREATE TABLE DiscountCodes (
    code_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE,
    kind NVARCHAR(10) NOT NULL,
    value DECIMAL(18,2) NOT NULL,
    holder NVARCHAR(100) NULL,
    max_uses INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Discount_Kind CHECK (kind IN ('PERCENT', 'FIXED')),
    CONSTRAINT CHK_Discount_Value CHECK (value > 0 AND (kind = 'FIXED' OR value <= 100)),
    CONSTRAINT CHK_Discount_Dates CHECK (valid_to >= valid_from)
);
GO

IF COL_LENGTH('Orders', 'discount_code') IS NULL
    ALTER TABLE Orders ADD discount_code NVARCHAR(30) NULL;
GO
IF COL_LENGTH('Orders', 'discount_amount') IS NULL
    ALTER TABLE Orders ADD discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_discount_amount DEFAULT 0;
GO

IF OBJECT_ID('Settings', 'U') IS NULL
CREATE TABLE Settings (
    setting_key NVARCHAR(50) NOT NULL PRIMARY KEY,
    setting_value NVARCHAR(100) NOT NULL
);
GO

IF OBJECT_ID('TaxRules', 'U') IS NULL
CREATE TABLE TaxRules (
    rule_id INT IDENTITY(1,1) PRIMARY KEY,
    kind NVARCHAR(20) NOT NULL,
    sector NVARCHAR(20) NULL,
    value DECIMAL(18,4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    source_ref NVARCHAR(200) NOT NULL,
    verified BIT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    updated_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_TaxRules_Kind CHECK (kind IN ('THRESHOLD', 'PIT_CEILING', 'VAT_RATE', 'PIT_RATE', 'VAT_REDUCTION', 'WARN_LEVEL')),
    CONSTRAINT CHK_TaxRules_Sector CHECK (sector IS NULL OR sector IN ('GOODS', 'SERVICE', 'PRODUCTION', 'OTHER')),
    CONSTRAINT CHK_TaxRules_Value CHECK (value >= 0),
    CONSTRAINT CHK_TaxRules_Dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
GO

IF COL_LENGTH('Orders', 'vat_rate') IS NULL
    ALTER TABLE Orders ADD vat_rate DECIMAL(5,2) NOT NULL CONSTRAINT DF_Orders_vat_rate DEFAULT 0;
GO
IF COL_LENGTH('Orders', 'vat_amount') IS NULL
    ALTER TABLE Orders ADD vat_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_vat_amount DEFAULT 0;
GO
IF COL_LENGTH('Orders', 'vat_included') IS NULL
    ALTER TABLE Orders ADD vat_included BIT NOT NULL CONSTRAINT DF_Orders_vat_included DEFAULT 1;
GO

IF OBJECT_ID('OrderItems', 'U') IS NULL
CREATE TABLE OrderItems (
    item_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NULL,
    product_name NVARCHAR(100) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE,
    CONSTRAINT FK_OrderItems_Products FOREIGN KEY (product_id) REFERENCES Products(product_id),
    CONSTRAINT CHK_OrderItem_Quantity CHECK (quantity > 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_OrderItems_OrderId' AND object_id = OBJECT_ID('OrderItems'))
CREATE INDEX IX_OrderItems_OrderId ON OrderItems(order_id);
GO

IF OBJECT_ID('Transactions', 'U') IS NULL
CREATE TABLE Transactions (
    trans_id INT IDENTITY(1,1) PRIMARY KEY,
    bank_trans_id NVARCHAR(100) NOT NULL UNIQUE,
    order_id INT NULL,
    amount DECIMAL(18,2) NOT NULL,
    content NVARCHAR(500) NOT NULL,
    bank_account NVARCHAR(50) NULL,
    gateway NVARCHAR(50) NOT NULL DEFAULT 'SEPAY',
    status NVARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    trans_time DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    received_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    announced BIT NOT NULL DEFAULT 0,
    printed BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_Transactions_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    CONSTRAINT CHK_Trans_Status CHECK (status IN ('COMPLETED', 'CANCELLED', 'LEGIT', 'DUPLICATE', 'NO_ORDER', 'ORDER_CLOSED'))
);
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_Trans_Status'
           AND definition NOT LIKE '%COMPLETED%')
BEGIN
    ALTER TABLE Transactions DROP CONSTRAINT CHK_Trans_Status;
    EXEC(N'UPDATE Transactions SET status = N''COMPLETED'' WHERE status = N''LEGIT'';
           UPDATE Transactions SET status = N''CANCELLED'' WHERE status IN (N''AMOUNT_MISMATCH'', N''DEVICE_OFFLINE'', N''PARTIAL'', N''ORDER_CLOSED'', N''NO_ORDER'', N''DUPLICATE'');');
    ALTER TABLE Transactions ADD CONSTRAINT CHK_Trans_Status
        CHECK (status IN ('COMPLETED', 'CANCELLED', 'LEGIT', 'DUPLICATE', 'NO_ORDER', 'ORDER_CLOSED'));
END
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_Device_Time' AND object_id = OBJECT_ID('Transactions'))
    DROP INDEX IX_Transactions_Device_Time ON Transactions;
IF COL_LENGTH('Transactions', 'device_id') IS NOT NULL
BEGIN
    ALTER TABLE Transactions DROP CONSTRAINT FK_Transactions_Devices;
    ALTER TABLE Transactions DROP COLUMN device_id;
END
GO
IF EXISTS (SELECT 1 FROM Devices WHERE device_type = N'SPEAKER')
BEGIN
    DELETE FROM Devices WHERE device_type = N'SPEAKER';
END
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_Device_Type' AND definition LIKE '%SPEAKER%')
BEGIN
    ALTER TABLE Devices DROP CONSTRAINT CHK_Device_Type;
    ALTER TABLE Devices ADD CONSTRAINT CHK_Device_Type CHECK (device_type IN ('PRINTER'));
END
GO

IF OBJECT_ID('FraudAlerts', 'U') IS NULL
CREATE TABLE FraudAlerts (
    alert_id INT IDENTITY(1,1) PRIMARY KEY,
    trans_id INT NOT NULL,
    rule_code NVARCHAR(50) NOT NULL,
    reason NVARCHAR(500) NOT NULL,
    severity NVARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status NVARCHAR(20) NOT NULL DEFAULT 'NEW',
    resolved_by INT NULL,
    resolved_notes NVARCHAR(500) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Alerts_Transactions FOREIGN KEY (trans_id) REFERENCES Transactions(trans_id),
    CONSTRAINT FK_Alerts_Users FOREIGN KEY (resolved_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Alert_Severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT CHK_Alert_Status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED'))
);
GO

IF OBJECT_ID('Refunds', 'U') IS NOT NULL DROP TABLE Refunds;
GO

IF OBJECT_ID('AuditLogs', 'U') IS NULL
CREATE TABLE AuditLogs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    actor_username NVARCHAR(50) NOT NULL,
    action NVARCHAR(50) NOT NULL,
    target_entity NVARCHAR(50) NOT NULL,
    entity_id NVARCHAR(50) NOT NULL,
    detail_json NVARCHAR(MAX) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME()
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_BankTransId' AND object_id = OBJECT_ID('Transactions'))
CREATE INDEX IX_Transactions_BankTransId ON Transactions(bank_trans_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_Announced' AND object_id = OBJECT_ID('Transactions'))
CREATE INDEX IX_Transactions_Announced ON Transactions(announced, status);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_Time' AND object_id = OBJECT_ID('Transactions'))
CREATE INDEX IX_Transactions_Time ON Transactions(trans_time);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Orders_OrderCode' AND object_id = OBJECT_ID('Orders'))
CREATE INDEX IX_Orders_OrderCode ON Orders(order_code);
GO

IF COL_LENGTH('Products', 'cost_price') IS NULL
ALTER TABLE Products ADD cost_price DECIMAL(18,2) NOT NULL CONSTRAINT DF_Products_Cost DEFAULT 0;
GO

IF COL_LENGTH('Products', 'stock_quantity') IS NULL
ALTER TABLE Products ADD stock_quantity INT NOT NULL CONSTRAINT DF_Products_Stock DEFAULT 0;
GO

IF OBJECT_ID('Affiliates', 'U') IS NULL
CREATE TABLE Affiliates (
    affiliate_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    rate DECIMAL(5,2) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Affiliate_Rate CHECK (rate >= 0 AND rate <= 100)
);
GO

IF OBJECT_ID('ChannelSales', 'U') IS NULL
CREATE TABLE ChannelSales (
    sale_id INT IDENTITY(1,1) PRIMARY KEY,
    channel NVARCHAR(20) NOT NULL,
    external_ref NVARCHAR(60) NOT NULL,
    sold_at DATETIME2(3) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    goods_amount DECIMAL(18,2) NOT NULL,
    platform_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    affiliate_id INT NULL,
    affiliate_commission DECIMAL(18,2) NOT NULL DEFAULT 0,
    cost_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    note NVARCHAR(200) NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_ChannelSales_Ref UNIQUE (channel, external_ref),
    CONSTRAINT FK_ChannelSales_Affiliates FOREIGN KEY (affiliate_id) REFERENCES Affiliates(affiliate_id),
    CONSTRAINT FK_ChannelSales_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_ChannelSales_Channel CHECK (channel IN ('SHOPEE', 'TIKTOK')),
    CONSTRAINT CHK_ChannelSales_Status CHECK (status IN ('COMPLETED', 'CANCELLED', 'RETURNED'))
);
GO

IF COL_LENGTH('ChannelSales', 'buyer_paid_amount') IS NULL
ALTER TABLE ChannelSales ADD buyer_paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0;
GO
IF COL_LENGTH('ChannelSales', 'seller_discount') IS NULL
ALTER TABLE ChannelSales ADD seller_discount DECIMAL(18,2) NOT NULL DEFAULT 0;
GO
IF COL_LENGTH('ChannelSales', 'platform_discount') IS NULL
ALTER TABLE ChannelSales ADD platform_discount DECIMAL(18,2) NOT NULL DEFAULT 0;
GO
IF COL_LENGTH('ChannelSales', 'service_fee') IS NULL
ALTER TABLE ChannelSales ADD service_fee DECIMAL(18,2) NOT NULL DEFAULT 0;
GO
IF COL_LENGTH('ChannelSales', 'transaction_fee') IS NULL
ALTER TABLE ChannelSales ADD transaction_fee DECIMAL(18,2) NOT NULL DEFAULT 0;
GO
IF COL_LENGTH('ChannelSales', 'settled_at') IS NULL
ALTER TABLE ChannelSales ADD settled_at DATETIME2(3) NULL;
GO
IF COL_LENGTH('ChannelSales', 'settlement_amount') IS NULL
ALTER TABLE ChannelSales ADD settlement_amount DECIMAL(18,2) NULL;
GO
IF COL_LENGTH('ChannelSales', 'settlement_status') IS NULL
ALTER TABLE ChannelSales ADD settlement_status NVARCHAR(20) NOT NULL DEFAULT 'SETTLED';
GO
IF COL_LENGTH('ChannelSales', 'traffic_source') IS NULL
ALTER TABLE ChannelSales ADD traffic_source NVARCHAR(50) NULL;
GO
IF COL_LENGTH('ChannelSales', 'cancel_reason') IS NULL
ALTER TABLE ChannelSales ADD cancel_reason NVARCHAR(255) NULL;
GO

IF OBJECT_ID('ChannelSaleItems', 'U') IS NULL
CREATE TABLE ChannelSaleItems (
    item_id INT IDENTITY(1,1) PRIMARY KEY,
    sale_id INT NOT NULL,
    sku_code NVARCHAR(50) NOT NULL,
    product_name NVARCHAR(255) NOT NULL,
    variation_name NVARCHAR(100) NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(18,2) NOT NULL DEFAULT 0,
    cost_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ChannelSaleItems_Sale FOREIGN KEY (sale_id) REFERENCES ChannelSales(sale_id) ON DELETE CASCADE
);
GO

IF OBJECT_ID('Invoices', 'U') IS NULL
CREATE TABLE Invoices (
    invoice_id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_no NVARCHAR(20) NOT NULL UNIQUE,
    source_type NVARCHAR(20) NOT NULL,
    source_id INT NOT NULL,
    buyer_name NVARCHAR(100) NOT NULL,
    buyer_tax_code NVARCHAR(20) NULL,
    buyer_address NVARCHAR(200) NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    vat_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    provider_invoice_no NVARCHAR(30) NULL,
    provider_lookup_code NVARCHAR(40) NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Invoices_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Invoice_Source CHECK (source_type IN ('ORDER', 'CHANNEL_SALE')),
    CONSTRAINT CHK_Invoice_Status CHECK (status IN ('DRAFT', 'SENT_TO_PROVIDER', 'ISSUED'))
);
GO

IF OBJECT_ID('InvoiceLines', 'U') IS NULL
CREATE TABLE InvoiceLines (
    line_id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_id INT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_InvoiceLines_Invoices FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id) ON DELETE CASCADE
);
GO

IF COL_LENGTH('Affiliates', 'referral_code') IS NULL
ALTER TABLE Affiliates ADD
    referral_code NVARCHAR(30) NULL,
    phone NVARCHAR(20) NULL,
    email NVARCHAR(100) NULL,
    bank_name NVARCHAR(60) NULL,
    bank_account NVARCHAR(30) NULL,
    bank_holder NVARCHAR(100) NULL,
    status NVARCHAR(12) NOT NULL CONSTRAINT DF_Affiliates_Status DEFAULT 'ACTIVE';
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_Affiliate_Status')
ALTER TABLE Affiliates ADD CONSTRAINT CHK_Affiliate_Status CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED'));
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Affiliates_Code' AND object_id = OBJECT_ID('Affiliates'))
CREATE UNIQUE INDEX UX_Affiliates_Code ON Affiliates(referral_code) WHERE referral_code IS NOT NULL;
GO

IF OBJECT_ID('Campaigns', 'U') IS NULL
CREATE TABLE Campaigns (
    campaign_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(80) NOT NULL UNIQUE,
    description NVARCHAR(300) NULL,
    commission_type NVARCHAR(10) NOT NULL,
    commission_value DECIMAL(18,2) NOT NULL DEFAULT 0,
    min_order DECIMAL(18,2) NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status NVARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Campaigns_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Campaign_Type CHECK (commission_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT CHK_Campaign_Status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT CHK_Campaign_Dates CHECK (end_date >= start_date),
    CONSTRAINT CHK_Campaign_Values CHECK (commission_value > 0 AND min_order >= 0)
);
GO

IF COL_LENGTH('Orders', 'referred_by_affiliate_id') IS NULL
ALTER TABLE Orders ADD referred_by_affiliate_id INT NULL, referral_campaign_id INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_Affiliates')
ALTER TABLE Orders ADD CONSTRAINT FK_Orders_Affiliates FOREIGN KEY (referred_by_affiliate_id) REFERENCES Affiliates(affiliate_id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_Campaigns')
ALTER TABLE Orders ADD CONSTRAINT FK_Orders_Campaigns FOREIGN KEY (referral_campaign_id) REFERENCES Campaigns(campaign_id);
GO

IF OBJECT_ID('Commissions', 'U') IS NULL
CREATE TABLE Commissions (
    commission_id INT IDENTITY(1,1) PRIMARY KEY,
    affiliate_id INT NOT NULL,
    order_id INT NOT NULL UNIQUE,
    campaign_id INT NOT NULL,
    order_amount DECIMAL(18,2) NOT NULL,
    eligible_amount DECIMAL(18,2) NOT NULL,
    rate_text NVARCHAR(40) NOT NULL,
    original_amount DECIMAL(18,2) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status NVARCHAR(12) NOT NULL DEFAULT 'PENDING',
    rule_snapshot NVARCHAR(300) NOT NULL,
    reason NVARCHAR(200) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Commissions_Affiliates FOREIGN KEY (affiliate_id) REFERENCES Affiliates(affiliate_id),
    CONSTRAINT FK_Commissions_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    CONSTRAINT FK_Commissions_Campaigns FOREIGN KEY (campaign_id) REFERENCES Campaigns(campaign_id),
    CONSTRAINT CHK_Commission_Status CHECK (status IN ('PENDING', 'APPROVED', 'AVAILABLE', 'ON_HOLD', 'REJECTED', 'PAID', 'REVERSED')),
    CONSTRAINT CHK_Commission_Amount CHECK (amount >= 0 AND original_amount >= 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Commissions_Affiliate' AND object_id = OBJECT_ID('Commissions'))
CREATE INDEX IX_Commissions_Affiliate ON Commissions(affiliate_id, status);
GO

IF OBJECT_ID('Payouts', 'U') IS NULL
CREATE TABLE Payouts (
    payout_id INT IDENTITY(1,1) PRIMARY KEY,
    affiliate_id INT NOT NULL,
    batch_code NVARCHAR(20) NULL,
    gross DECIMAL(18,2) NOT NULL,
    deduction DECIMAL(18,2) NOT NULL DEFAULT 0,
    net DECIMAL(18,2) NOT NULL,
    status NVARCHAR(10) NOT NULL DEFAULT 'REQUESTED',
    bank_info NVARCHAR(200) NULL,
    reference NVARCHAR(60) NULL,
    requested_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    paid_at DATETIME2(3) NULL,
    paid_by INT NULL,
    CONSTRAINT FK_Payouts_Affiliates FOREIGN KEY (affiliate_id) REFERENCES Affiliates(affiliate_id),
    CONSTRAINT FK_Payouts_Users FOREIGN KEY (paid_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Payout_Status CHECK (status IN ('REQUESTED', 'PAID')),
    CONSTRAINT CHK_Payout_Amounts CHECK (deduction >= 0)
);
GO

IF COL_LENGTH('Commissions', 'payout_id') IS NULL
ALTER TABLE Commissions ADD payout_id INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Commissions_Payouts')
ALTER TABLE Commissions ADD CONSTRAINT FK_Commissions_Payouts FOREIGN KEY (payout_id) REFERENCES Payouts(payout_id);
GO

IF OBJECT_ID('CommissionAdjustments', 'U') IS NULL
CREATE TABLE CommissionAdjustments (
    adjustment_id INT IDENTITY(1,1) PRIMARY KEY,
    commission_id INT NOT NULL,
    affiliate_id INT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    reason NVARCHAR(200) NOT NULL,
    payout_id INT NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_CommAdj_Commissions FOREIGN KEY (commission_id) REFERENCES Commissions(commission_id),
    CONSTRAINT FK_CommAdj_Affiliates FOREIGN KEY (affiliate_id) REFERENCES Affiliates(affiliate_id),
    CONSTRAINT FK_CommAdj_Payouts FOREIGN KEY (payout_id) REFERENCES Payouts(payout_id),
    CONSTRAINT FK_CommAdj_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_CommAdj_Amount CHECK (amount <> 0)
);
GO

IF OBJECT_ID('TaxAssessments', 'U') IS NULL
CREATE TABLE TaxAssessments (
    assessment_id INT IDENTITY(1,1) PRIMARY KEY,
    assessed_on DATE NOT NULL,
    period_label NVARCHAR(40) NOT NULL,
    summary NVARCHAR(500) NOT NULL,
    verification NVARCHAR(20) NOT NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxAssessments_Users FOREIGN KEY (created_by) REFERENCES Users(user_id)
);
GO

IF OBJECT_ID('TaxCalculations', 'U') IS NULL
CREATE TABLE TaxCalculations (
    calc_id INT IDENTITY(1,1) PRIMARY KEY,
    calc_date DATE NOT NULL,
    period_label NVARCHAR(40) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    sector NVARCHAR(20) NOT NULL,
    revenue DECIMAL(18,2) NOT NULL,
    adjustment DECIMAL(18,2) NOT NULL DEFAULT 0,
    vat_amount DECIMAL(18,2) NOT NULL,
    pit_amount DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    rule_stamp NVARCHAR(300) NOT NULL,
    verified_rules BIT NOT NULL DEFAULT 0,
    note NVARCHAR(200) NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxCalculations_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_TaxCalc_Adjustment CHECK (adjustment >= 0 AND adjustment <= revenue)
);
GO

IF OBJECT_ID('TaxObligations', 'U') IS NULL
CREATE TABLE TaxObligations (
    obligation_id INT IDENTITY(1,1) PRIMARY KEY,
    kind NVARCHAR(20) NOT NULL,
    period_label NVARCHAR(40) NOT NULL,
    due_date DATE NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'NOT_ASSESSED',
    responsible_user INT NULL,
    estimated_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    declared_amount DECIMAL(18,2) NULL,
    confirmed_amount DECIMAL(18,2) NULL,
    submission_ref NVARCHAR(60) NULL,
    completed_on DATE NULL,
    notes NVARCHAR(300) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxObligations_Users FOREIGN KEY (responsible_user) REFERENCES Users(user_id),
    CONSTRAINT CHK_TaxObl_Kind CHECK (kind IN ('VAT', 'PIT', 'EINVOICE', 'BOOKKEEPING')),
    CONSTRAINT CHK_TaxObl_Status CHECK (status IN ('NOT_ASSESSED', 'NEEDS_INFO', 'APPLICABLE', 'NOT_APPLICABLE', 'PREPARATION', 'READY_FOR_REVIEW', 'SUBMITTED', 'AWAITING_PAYMENT', 'PAID', 'OVERDUE', 'NEEDS_ADJUSTMENT'))
);
GO

IF OBJECT_ID('TaxObligationHistory', 'U') IS NULL
CREATE TABLE TaxObligationHistory (
    history_id INT IDENTITY(1,1) PRIMARY KEY,
    obligation_id INT NOT NULL,
    from_status NVARCHAR(20) NOT NULL,
    to_status NVARCHAR(20) NOT NULL,
    note NVARCHAR(300) NOT NULL,
    changed_by INT NOT NULL,
    changed_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxHist_Obligations FOREIGN KEY (obligation_id) REFERENCES TaxObligations(obligation_id),
    CONSTRAINT FK_TaxHist_Users FOREIGN KEY (changed_by) REFERENCES Users(user_id)
);
GO

IF OBJECT_ID('TaxPayments', 'U') IS NULL
CREATE TABLE TaxPayments (
    payment_id INT IDENTITY(1,1) PRIMARY KEY,
    obligation_id INT NOT NULL,
    paid_on DATE NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    method NVARCHAR(30) NOT NULL,
    reference NVARCHAR(60) NULL,
    receipt_name NVARCHAR(100) NULL,
    recorded_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxPayments_Obligations FOREIGN KEY (obligation_id) REFERENCES TaxObligations(obligation_id),
    CONSTRAINT FK_TaxPayments_Users FOREIGN KEY (recorded_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_TaxPay_Amount CHECK (amount > 0),
    CONSTRAINT CHK_TaxPay_Evidence CHECK (reference IS NOT NULL OR receipt_name IS NOT NULL)
);
GO

IF OBJECT_ID('Expenses', 'U') IS NULL
CREATE TABLE Expenses (
    expense_id INT IDENTITY(1,1) PRIMARY KEY,
    spent_on DATE NOT NULL,
    supplier NVARCHAR(100) NOT NULL,
    supplier_tax_code NVARCHAR(20) NULL,
    category NVARCHAR(30) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    payment_method NVARCHAR(30) NOT NULL,
    invoice_no NVARCHAR(40) NULL,
    has_document BIT NOT NULL DEFAULT 0,
    tax_treatment NVARCHAR(20) NOT NULL DEFAULT 'UNCLASSIFIED',
    note NVARCHAR(200) NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Expenses_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Expense_Amount CHECK (amount > 0),
    CONSTRAINT CHK_Expense_Treatment CHECK (tax_treatment IN ('UNCLASSIFIED', 'MAY_COUNT', 'NOT_COUNTED'))
);
GO

IF OBJECT_ID('TaxReconNotes', 'U') IS NULL
CREATE TABLE TaxReconNotes (
    note_id INT IDENTITY(1,1) PRIMARY KEY,
    ref_key NVARCHAR(40) NOT NULL UNIQUE,
    note NVARCHAR(300) NOT NULL,
    resolved BIT NOT NULL DEFAULT 0,
    updated_by INT NOT NULL,
    updated_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxRecon_Users FOREIGN KEY (updated_by) REFERENCES Users(user_id)
);
GO

IF OBJECT_ID('TaxDocuments', 'U') IS NULL
CREATE TABLE TaxDocuments (
    doc_id INT IDENTITY(1,1) PRIMARY KEY,
    doc_name NVARCHAR(150) NOT NULL,
    category NVARCHAR(20) NOT NULL,
    period_label NVARCHAR(40) NULL,
    status NVARCHAR(12) NOT NULL DEFAULT 'MISSING',
    file_name NVARCHAR(150) NULL,
    content_type NVARCHAR(100) NULL,
    size_bytes INT NOT NULL DEFAULT 0,
    content VARBINARY(MAX) NULL,
    uploaded_by INT NULL,
    uploaded_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TaxDocuments_Users FOREIGN KEY (uploaded_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_TaxDocuments_Status CHECK (status IN ('UPLOADED', 'MISSING', 'REVIEW')),
    CONSTRAINT CHK_TaxDocuments_Category CHECK (category IN ('REGISTRATION', 'TAX_CODE', 'INVOICE', 'BANK', 'EXPENSE', 'FILING', 'OTHER')),
    CONSTRAINT CHK_TaxDocuments_Size CHECK (size_bytes >= 0 AND size_bytes <= 10485760)
);
GO

IF OBJECT_ID('ChannelPayouts', 'U') IS NULL
CREATE TABLE ChannelPayouts (
    payout_id INT IDENTITY(1,1) PRIMARY KEY,
    reference NVARCHAR(20) NOT NULL UNIQUE,
    affiliate_id INT NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    payment_method NVARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_reference NVARCHAR(60) NULL,
    payment_date DATE NULL,
    status NVARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    notes NVARCHAR(200) NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    confirmed_by INT NULL,
    confirmed_at DATETIME2(3) NULL,
    CONSTRAINT FK_ChPayouts_Affiliates FOREIGN KEY (affiliate_id) REFERENCES Affiliates(affiliate_id),
    CONSTRAINT FK_ChPayouts_Creator FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT FK_ChPayouts_Confirmer FOREIGN KEY (confirmed_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_ChPayouts_Status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT CHK_ChPayouts_Method CHECK (payment_method IN ('BANK_TRANSFER', 'CASH', 'OTHER')),
    CONSTRAINT CHK_ChPayouts_Total CHECK (total_amount >= 0)
);
GO

IF OBJECT_ID('ChannelCommissions', 'U') IS NULL
CREATE TABLE ChannelCommissions (
    commission_id INT IDENTITY(1,1) PRIMARY KEY,
    affiliate_id INT NOT NULL,
    channel NVARCHAR(20) NOT NULL,
    external_ref NVARCHAR(60) NOT NULL,
    platform_order_id NVARCHAR(60) NOT NULL,
    order_date DATE NOT NULL,
    base_amount DECIMAL(18,2) NOT NULL,
    commission_type NVARCHAR(10) NOT NULL,
    commission_rate DECIMAL(18,2) NOT NULL,
    calculated_amount DECIMAL(18,2) NOT NULL,
    reported_amount DECIMAL(18,2) NULL,
    approved_amount DECIMAL(18,2) NULL,
    status NVARCHAR(10) NOT NULL DEFAULT 'PENDING',
    data_source NVARCHAR(12) NOT NULL,
    attribution NVARCHAR(100) NULL,
    notes NVARCHAR(200) NULL,
    reject_reason NVARCHAR(200) NULL,
    payout_id INT NULL,
    created_by INT NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    approved_by INT NULL,
    approved_at DATETIME2(3) NULL,
    CONSTRAINT UQ_ChCommissions_Ref UNIQUE (channel, external_ref),
    CONSTRAINT FK_ChCommissions_Affiliates FOREIGN KEY (affiliate_id) REFERENCES Affiliates(affiliate_id),
    CONSTRAINT FK_ChCommissions_Payouts FOREIGN KEY (payout_id) REFERENCES ChannelPayouts(payout_id),
    CONSTRAINT FK_ChCommissions_Creator FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT FK_ChCommissions_Approver FOREIGN KEY (approved_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_ChCommissions_Channel CHECK (channel IN ('SHOPEE', 'TIKTOK')),
    CONSTRAINT CHK_ChCommissions_Type CHECK (commission_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT CHK_ChCommissions_Status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED')),
    CONSTRAINT CHK_ChCommissions_Source CHECK (data_source IN ('MANUAL', 'CSV_IMPORT')),
    CONSTRAINT CHK_ChCommissions_Amounts CHECK (base_amount > 0 AND commission_rate >= 0 AND calculated_amount >= 0)
);
GO

IF OBJECT_ID('ChannelPayoutItems', 'U') IS NULL
CREATE TABLE ChannelPayoutItems (
    item_id INT IDENTITY(1,1) PRIMARY KEY,
    payout_id INT NOT NULL,
    commission_id INT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_ChPayItems_Payouts FOREIGN KEY (payout_id) REFERENCES ChannelPayouts(payout_id),
    CONSTRAINT FK_ChPayItems_Commissions FOREIGN KEY (commission_id) REFERENCES ChannelCommissions(commission_id),
    CONSTRAINT UQ_ChPayItems UNIQUE (payout_id, commission_id),
    CONSTRAINT CHK_ChPayItems_Amount CHECK (amount > 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ChCommissions_Affiliate' AND object_id = OBJECT_ID('ChannelCommissions'))
CREATE INDEX IX_ChCommissions_Affiliate ON ChannelCommissions(affiliate_id, status);
GO

IF OBJECT_ID('GuideProgress', 'U') IS NULL
CREATE TABLE GuideProgress (
    progress_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    article_id NVARCHAR(100) NOT NULL,
    kind NVARCHAR(20) NOT NULL,
    state NVARCHAR(50) NOT NULL,
    updated_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_GuideProgress_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT UQ_GuideProgress UNIQUE (user_id, article_id, kind)
);
GO
