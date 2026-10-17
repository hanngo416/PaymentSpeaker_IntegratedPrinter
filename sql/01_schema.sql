-- =====================================================================================
-- SMART PAYMENT NOTIFICATION SPEAKER WITH INTEGRATED RECEIPT PRINTER
-- Database Setup Script (SQL Server 2019)
-- PRJ301 project - 3NF design, idempotency and audit log
-- =====================================================================================

USE master;
GO

-- 1. CREATE THE DATABASE IF IT DOES NOT EXIST
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

-- Tables are only created when they do not exist yet, so re-running this script keeps real data.
-- To wipe everything (demo reset), run 99_reset_demo.sql first.
GO

-- =====================================================================================
-- 2. TABLE DEFINITIONS (DDL)
-- =====================================================================================

-- TABLE 1: ROLES (RBAC)
IF OBJECT_ID('Roles', 'U') IS NULL
CREATE TABLE Roles (
    role_id INT PRIMARY KEY,
    role_name NVARCHAR(50) NOT NULL UNIQUE,      -- ADMIN, STORE_MANAGER, CASHIER, AUDITOR, TECHNICIAN
    description NVARCHAR(255) NULL
);
GO

-- TABLE 2: USERS (staff accounts)
IF OBJECT_ID('Users', 'U') IS NULL
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    user_code NVARCHAR(50) NOT NULL UNIQUE,       -- Staff code for display and logs, unique to guarantee audit trail integrity; self-registered accounts get the part of the email before @, plus a number when it is already taken
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(256) NOT NULL UNIQUE,
    google_id NVARCHAR(64) NULL,
    role_id INT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, LOCKED, PENDING (self-registered with Google, waits for an admin to approve and give a role)
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Users_google_id' AND object_id = OBJECT_ID('Users'))
CREATE UNIQUE INDEX UX_Users_google_id ON Users(google_id) WHERE google_id IS NOT NULL;
GO

-- TABLE 2B: LOGIN_OTPS (one-time login codes sent by email)
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

-- TABLE 3: DEVICES (printers at the counter)
IF OBJECT_ID('Devices', 'U') IS NULL
CREATE TABLE Devices (
    device_id INT IDENTITY(1,1) PRIMARY KEY,
    device_code NVARCHAR(50) NOT NULL UNIQUE,    -- PRN_01
    device_type NVARCHAR(20) NOT NULL,           -- PRINTER
    model_name NVARCHAR(100) NOT NULL,           -- "Aiyin A70"
    api_key NVARCHAR(100) NOT NULL UNIQUE,       -- Secret key identifying the device
    status NVARCHAR(20) NOT NULL DEFAULT 'ONLINE', -- ONLINE, OFFLINE, MAINTENANCE
    last_ping DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Device_Type CHECK (device_type IN ('PRINTER'))
);
GO

-- Databases created before the tickets were removed
IF OBJECT_ID('MaintenanceTickets', 'U') IS NOT NULL DROP TABLE MaintenanceTickets;
GO

-- TABLE 5: ORDERS (payment requests and VietQR)
IF OBJECT_ID('Orders', 'U') IS NULL
CREATE TABLE Orders (
    order_id INT IDENTITY(1,1) PRIMARY KEY,
    order_code NVARCHAR(50) NOT NULL UNIQUE,     -- VietQR reference code, for example TINKY10234
    created_by INT NOT NULL,                     -- Cashier who created the order
    created_by_code NVARCHAR(50) NOT NULL,       -- Copy of that cashier user_code at creation time, so a plain SELECT on Orders shows who made it
    amount DECIMAL(18,2) NOT NULL,               -- Amount requested (total order amount)
    cash_amount DECIMAL(18,2) NOT NULL DEFAULT 0, -- Portion paid in cash at counter; the VietQR asks for amount - cash_amount
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, CANCELLED, EXPIRED
    qr_content NVARCHAR(500) NULL,               -- Payload string used to build the VietQR code
    expires_at DATETIME2(3) NOT NULL,            -- Scan deadline (usually 10-15 minutes)
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Orders_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Order_Status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'PAID'))
);
GO

-- Databases created before the cash portion existed: add cash_amount
IF COL_LENGTH('Orders', 'cash_amount') IS NULL
BEGIN
    ALTER TABLE Orders ADD cash_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_cash_amount DEFAULT 0;
END
GO

-- Databases that still have paid_amount (part payments were removed: the QR amount is fixed, a customer pays the rest in cash)
IF COL_LENGTH('Orders', 'paid_amount') IS NOT NULL
BEGIN
    DECLARE @df NVARCHAR(200) = (SELECT dc.name FROM sys.default_constraints dc
        JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID('Orders') AND c.name = 'paid_amount');
    IF @df IS NOT NULL EXEC(N'ALTER TABLE Orders DROP CONSTRAINT ' + @df);
    ALTER TABLE Orders DROP COLUMN paid_amount;
END
GO

-- TABLE 5b: DISCOUNT CODES (holder NULL = shared code the cashier picks from a list; holder set = personal code typed by hand)
IF OBJECT_ID('DiscountCodes', 'U') IS NULL
CREATE TABLE DiscountCodes (
    code_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE,
    kind NVARCHAR(10) NOT NULL,                  -- PERCENT or FIXED
    value DECIMAL(18,2) NOT NULL,                -- Percent (1-100) or amount in VND
    holder NVARCHAR(100) NULL,                   -- Person the personal code was issued to
    max_uses INT NULL,                           -- NULL = unlimited
    used_count INT NOT NULL DEFAULT 0,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,                      -- Usable through the end of this day
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Discount_Kind CHECK (kind IN ('PERCENT', 'FIXED')),
    CONSTRAINT CHK_Discount_Value CHECK (value > 0 AND (kind = 'FIXED' OR value <= 100)),
    CONSTRAINT CHK_Discount_Dates CHECK (valid_to >= valid_from)
);
GO

-- Databases created before discount codes existed: store the code and the amount taken off on each order
IF COL_LENGTH('Orders', 'discount_code') IS NULL
    ALTER TABLE Orders ADD discount_code NVARCHAR(30) NULL;
GO
IF COL_LENGTH('Orders', 'discount_amount') IS NULL
    ALTER TABLE Orders ADD discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_discount_amount DEFAULT 0;
GO

-- TABLE 6: TRANSACTIONS (balance changes pulled from the SePay API by the polling job)
IF OBJECT_ID('Transactions', 'U') IS NULL
CREATE TABLE Transactions (
    trans_id INT IDENTITY(1,1) PRIMARY KEY,
    bank_trans_id NVARCHAR(100) NOT NULL UNIQUE, -- Bank transaction id (idempotency key against the same transaction pulled again)
    order_id INT NULL,                           -- Matched order (if a TINKY... code was found)
    amount DECIMAL(18,2) NOT NULL,               -- Amount actually received
    content NVARCHAR(500) NOT NULL,              -- Transfer note typed by the customer
    bank_account NVARCHAR(50) NULL,              -- Receiving account number
    gateway NVARCHAR(50) NOT NULL DEFAULT 'SEPAY',
    status NVARCHAR(50) NOT NULL DEFAULT 'COMPLETED', -- COMPLETED, CANCELLED, LEGIT, DUPLICATE, NO_ORDER, ORDER_CLOSED
    trans_time DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),-- Time recorded by the bank
    received_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),-- Time the server received the transaction
    announced BIT NOT NULL DEFAULT 0,            -- 0: payment popup not yet confirmed by the cashier, 1: confirmed (prevents repeats)
    printed BIT NOT NULL DEFAULT 0,              -- 0: receipt not printed, 1: receipt printed
    CONSTRAINT FK_Transactions_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    CONSTRAINT CHK_Trans_Status CHECK (status IN ('COMPLETED', 'CANCELLED', 'LEGIT', 'DUPLICATE', 'NO_ORDER', 'ORDER_CLOSED'))
);
GO

-- Databases created with older status names: ensure COMPLETED and CANCELLED are allowed
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

-- Databases that still have the speaker device: the computer speaker replaced it, so drop the device column of Transactions and the SPEAKER devices
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

-- TABLE 7: FRAUD_ALERTS (fraud alerts raised by the rules engine)
IF OBJECT_ID('FraudAlerts', 'U') IS NULL
CREATE TABLE FraudAlerts (
    alert_id INT IDENTITY(1,1) PRIMARY KEY,
    trans_id INT NOT NULL,
    rule_code NVARCHAR(50) NOT NULL,             -- DUP_TRANS, NO_ORDER, AMOUNT_MISMATCH, ORDER_NOT_PENDING, HIGH_AMOUNT, BURST_RATE
    reason NVARCHAR(500) NOT NULL,               -- Detailed reason for the alert
    severity NVARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH
    status NVARCHAR(20) NOT NULL DEFAULT 'NEW',  -- NEW, IN_PROGRESS, RESOLVED, DISMISSED
    resolved_by INT NULL,                        -- Auditor who handled it
    resolved_notes NVARCHAR(500) NULL,           -- How it was resolved
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Alerts_Transactions FOREIGN KEY (trans_id) REFERENCES Transactions(trans_id),
    CONSTRAINT FK_Alerts_Users FOREIGN KEY (resolved_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Alert_Severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT CHK_Alert_Status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED'))
);
GO

-- Databases that still have Refunds: money that does not match an order is only flagged now, nothing is sent back from the system
IF OBJECT_ID('Refunds', 'U') IS NOT NULL DROP TABLE Refunds;
GO

-- TABLE 8: AUDIT_LOGS (audit trail: who did what, when)
IF OBJECT_ID('AuditLogs', 'U') IS NULL
CREATE TABLE AuditLogs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    actor_username NVARCHAR(50) NOT NULL,
    action NVARCHAR(50) NOT NULL,                -- LOGIN, LOGOUT, CREATE_ORDER, TRANSACTION_INGEST, UPDATE_STATUS
    target_entity NVARCHAR(50) NOT NULL,         -- USER, ORDER, TRANSACTION, DEVICE
    entity_id NVARCHAR(50) NOT NULL,
    detail_json NVARCHAR(MAX) NULL,              -- Changed content as a JSON diff
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME()
);
GO

-- =====================================================================================
-- 3. INDEXES FOR FREQUENT OPERATIONS
-- =====================================================================================
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_BankTransId' AND object_id = OBJECT_ID('Transactions'))
CREATE INDEX IX_Transactions_BankTransId ON Transactions(bank_trans_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_Announced' AND object_id = OBJECT_ID('Transactions'))
CREATE INDEX IX_Transactions_Announced ON Transactions(announced, status);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Transactions_Time' AND object_id = OBJECT_ID('Transactions'))
CREATE INDEX IX_Transactions_Time ON Transactions(trans_time);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Orders_OrderCode' AND object_id = OBJECT_ID('Orders'))
CREATE INDEX IX_Orders_OrderCode ON Orders(order_code);
GO
