-- =====================================================================================
-- HỆ THỐNG LOA THÔNG BÁO CHUYỂN TIỀN THÔNG MINH TÍCH HỢP IN HÓA ĐƠN
-- Database Setup Script (SQL Server 2019)
-- Đồ án PRJ301 - Chuẩn hóa 3NF, Idempotency & Audit Hash Chain
-- =====================================================================================

USE master;
GO

-- 1. TẠO DATABASE NẾU CHƯA CÓ
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'PaymentSpeaker')
BEGIN
    CREATE DATABASE PaymentSpeaker;
    PRINT N'>>> Đã tạo database PaymentSpeaker thành công!';
END
GO

USE PaymentSpeaker;
GO

-- Xóa các bảng cũ nếu đã tồn tại (thứ tự xóa từ bảng con đến bảng cha)
IF OBJECT_ID('AuditLogs', 'U') IS NOT NULL DROP TABLE AuditLogs;
IF OBJECT_ID('FraudAlerts', 'U') IS NOT NULL DROP TABLE FraudAlerts;
IF OBJECT_ID('Transactions', 'U') IS NOT NULL DROP TABLE Transactions;
IF OBJECT_ID('Orders', 'U') IS NOT NULL DROP TABLE Orders;
IF OBJECT_ID('MaintenanceTickets', 'U') IS NOT NULL DROP TABLE MaintenanceTickets;
IF OBJECT_ID('Devices', 'U') IS NOT NULL DROP TABLE Devices;
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
IF OBJECT_ID('Roles', 'U') IS NOT NULL DROP TABLE Roles;
IF OBJECT_ID('Stores', 'U') IS NOT NULL DROP TABLE Stores;
GO

-- =====================================================================================
-- 2. ĐỊNH NGHĨA CÁC BẢNG (DDL)
-- =====================================================================================

-- BẢNG 1: STORES (Quản lý chi nhánh)
CREATE TABLE Stores (
    store_id INT IDENTITY(1,1) PRIMARY KEY,
    store_code NVARCHAR(50) NOT NULL UNIQUE,     -- Ví dụ: STORE_HN01, STORE_SG01
    store_name NVARCHAR(150) NOT NULL,           -- Tên cửa hàng / chi nhánh
    address NVARCHAR(255) NOT NULL,              -- Địa chỉ
    phone NVARCHAR(20) NULL,                     -- Số điện thoại liên hệ
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE (Mở), INACTIVE (Đóng)
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME()
);
GO

-- BẢNG 2: ROLES (Phân quyền RBAC)
CREATE TABLE Roles (
    role_id INT PRIMARY KEY,
    role_name NVARCHAR(50) NOT NULL UNIQUE,      -- ADMIN, STORE_MANAGER, CASHIER, AUDITOR, TECHNICIAN
    description NVARCHAR(255) NULL
);
GO

-- BẢNG 3: USERS (Tài khoản nhân sự)
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password_hash NVARCHAR(256) NOT NULL,        -- Chuỗi băm mật khẩu (PBKDF2 / SHA-256)
    salt NVARCHAR(64) NOT NULL,                  -- Salt riêng cho từng user để chống Rainbow Table
    full_name NVARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    store_id INT NULL,                           -- Thu ngân/Quản lý gắn với Store; Admin/Auditor = NULL
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, LOCKED
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id),
    CONSTRAINT FK_Users_Stores FOREIGN KEY (store_id) REFERENCES Stores(store_id)
);
GO

-- BẢNG 4: DEVICES (Thiết bị Loa & Máy in tại quầy)
CREATE TABLE Devices (
    device_id INT IDENTITY(1,1) PRIMARY KEY,
    device_code NVARCHAR(50) NOT NULL UNIQUE,    -- SPK_HN01, PRN_HN01
    device_type NVARCHAR(20) NOT NULL,           -- SPEAKER, PRINTER
    model_name NVARCHAR(100) NOT NULL,           -- "Aiyin A70", "Smart Speaker V2"
    store_id INT NOT NULL,
    api_key NVARCHAR(100) NOT NULL UNIQUE,       -- Khóa bí mật định danh thiết bị / cửa hàng
    status NVARCHAR(20) NOT NULL DEFAULT 'ONLINE', -- ONLINE, OFFLINE, MAINTENANCE
    last_ping DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Devices_Stores FOREIGN KEY (store_id) REFERENCES Stores(store_id),
    CONSTRAINT CHK_Device_Type CHECK (device_type IN ('SPEAKER', 'PRINTER'))
);
GO

-- BẢNG 5: MAINTENANCE_TICKETS (Phiếu sự cố & Bảo trì phần cứng)
CREATE TABLE MaintenanceTickets (
    ticket_id INT IDENTITY(1,1) PRIMARY KEY,
    device_id INT NOT NULL,
    reported_by INT NOT NULL,                    -- Thu ngân / Quản lý tạo phiếu
    issue_description NVARCHAR(500) NOT NULL,    -- Ví dụ: "Máy in kẹt dao cắt", "Loa mất tiếng"
    status NVARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED
    tech_notes NVARCHAR(500) NULL,               -- Ghi chú kỹ thuật viên
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    resolved_at DATETIME2(3) NULL,
    CONSTRAINT FK_Tickets_Devices FOREIGN KEY (device_id) REFERENCES Devices(device_id),
    CONSTRAINT FK_Tickets_Users FOREIGN KEY (reported_by) REFERENCES Users(user_id)
);
GO

-- BẢNG 6: ORDERS (Yêu cầu thanh toán đơn hàng & VietQR)
CREATE TABLE Orders (
    order_id INT IDENTITY(1,1) PRIMARY KEY,
    order_code NVARCHAR(50) NOT NULL UNIQUE,     -- Mã tham chiếu VietQR, ví dụ: TINKY10234
    store_id INT NOT NULL,
    created_by INT NOT NULL,                     -- Thu ngân tạo đơn
    amount DECIMAL(18,2) NOT NULL,               -- Số tiền yêu cầu thanh toán
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, EXPIRED, CANCELLED
    qr_content NVARCHAR(500) NULL,               -- Chuỗi payload tạo mã QR VietQR
    expires_at DATETIME2(3) NOT NULL,                -- Thời hạn quét (thường 10-15 phút)
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Orders_Stores FOREIGN KEY (store_id) REFERENCES Stores(store_id),
    CONSTRAINT FK_Orders_Users FOREIGN KEY (created_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Order_Status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED'))
);
GO

-- BẢNG 7: TRANSACTIONS (Biến động số dư nhận từ Webhook Ngân hàng / SePay)
CREATE TABLE Transactions (
    trans_id INT IDENTITY(1,1) PRIMARY KEY,
    bank_trans_id NVARCHAR(100) NOT NULL UNIQUE, -- ID giao dịch của Ngân hàng (Idempotency Key chống trùng lặp Webhook)
    order_id INT NULL,                           -- Đơn hàng được khớp (nếu tìm thấy mã TINKY...)
    store_id INT NULL,                           -- Cửa hàng thụ hưởng
    amount DECIMAL(18,2) NOT NULL,               -- Số tiền thực tế nhận được
    content NVARCHAR(500) NOT NULL,              -- Nội dung chuyển khoản khách nhập
    bank_account NVARCHAR(50) NULL,              -- Số tài khoản nhận
    gateway NVARCHAR(50) NOT NULL DEFAULT 'SEPAY',
    status NVARCHAR(50) NOT NULL DEFAULT 'LEGIT',-- LEGIT, DUPLICATE, AMOUNT_MISMATCH, SUSPECTED_FRAUD, DEVICE_OFFLINE
    trans_time DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),-- Thời gian ngân hàng ghi nhận
    received_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),-- Thời điểm server nhận webhook
    announced BIT NOT NULL DEFAULT 0,            -- 0: Chưa đọc loa, 1: Đã đọc (chống đọc lại)
    printed BIT NOT NULL DEFAULT 0,              -- 0: Chưa in bill, 1: Đã in bill
    CONSTRAINT FK_Transactions_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    CONSTRAINT FK_Transactions_Stores FOREIGN KEY (store_id) REFERENCES Stores(store_id),
    CONSTRAINT CHK_Trans_Status CHECK (status IN ('LEGIT', 'DUPLICATE', 'AMOUNT_MISMATCH', 'SUSPECTED_FRAUD', 'DEVICE_OFFLINE'))
);
GO

-- BẢNG 8: FRAUD_ALERTS (Cảnh báo gian lận & Tra soát từ Rules Engine)
CREATE TABLE FraudAlerts (
    alert_id INT IDENTITY(1,1) PRIMARY KEY,
    trans_id INT NOT NULL,
    rule_code NVARCHAR(50) NOT NULL,             -- DUP_TRANS, AMOUNT_MISMATCH, HIGH_AMOUNT, BURST_RATE
    reason NVARCHAR(500) NOT NULL,               -- Lý do cảnh báo chi tiết
    severity NVARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH
    status NVARCHAR(20) NOT NULL DEFAULT 'NEW',  -- NEW, IN_PROGRESS, RESOLVED, DISMISSED
    resolved_by INT NULL,                        -- Auditor xử lý
    resolved_notes NVARCHAR(500) NULL,           -- Ghi chú cách giải quyết
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Alerts_Transactions FOREIGN KEY (trans_id) REFERENCES Transactions(trans_id),
    CONSTRAINT FK_Alerts_Users FOREIGN KEY (resolved_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Alert_Severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT CHK_Alert_Status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED'))
);
GO

-- BẢNG 9: AUDIT_LOGS (Nhật ký kiểm toán toàn vẹn - Hash Chain / Mini-Blockchain)
CREATE TABLE AuditLogs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    actor_username NVARCHAR(50) NOT NULL,
    action NVARCHAR(50) NOT NULL,                -- LOGIN, LOGOUT, CREATE_ORDER, WEBHOOK_INGEST, UPDATE_STATUS
    target_entity NVARCHAR(50) NOT NULL,         -- USER, ORDER, TRANSACTION, STORE, DEVICE
    entity_id NVARCHAR(50) NOT NULL,
    detail_json NVARCHAR(MAX) NULL,              -- Nội dung thay đổi dạng JSON diff
    prev_hash CHAR(64) NOT NULL,                 -- SHA-256 của bản ghi liền trước (chuỗi băm bảo vệ)
    curr_hash CHAR(64) NOT NULL,                 -- SHA-256(prev_hash + log_id + actor + action + entity + detail + created_at)
    created_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME()
);
GO

-- =====================================================================================
-- 3. TẠO INDEXES TỐI ƯU HIỆU NĂNG CHO CÁC TÁC VỤ THƯỜNG XUYÊN
-- =====================================================================================
CREATE INDEX IX_Transactions_BankTransId ON Transactions(bank_trans_id);
CREATE INDEX IX_Transactions_Announced ON Transactions(announced, status, store_id);
CREATE INDEX IX_Orders_OrderCode ON Orders(order_code);
CREATE INDEX IX_AuditLogs_PrevHash ON AuditLogs(prev_hash);
GO

