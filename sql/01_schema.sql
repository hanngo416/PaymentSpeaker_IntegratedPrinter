
USE master;
GO

IF DB_ID('PaymentSpeakerDB') IS NOT NULL
BEGIN
    ALTER DATABASE PaymentSpeakerDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE PaymentSpeakerDB;
END;
GO

CREATE DATABASE PaymentSpeakerDB;
GO

USE PaymentSpeakerDB;
GO

CREATE TABLE Users (
    user_id       INT IDENTITY PRIMARY KEY,
    username      NVARCHAR(50) UNIQUE NOT NULL,
    password_hash NVARCHAR(200) NOT NULL,
    full_name     NVARCHAR(80),
    status        NVARCHAR(20) DEFAULT 'ACTIVE',
    created_at    DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE Roles (
    role_id   INT IDENTITY PRIMARY KEY,
    role_name NVARCHAR(30) UNIQUE NOT NULL
);
GO

CREATE TABLE UserRole (
    user_id INT REFERENCES Users,
    role_id INT REFERENCES Roles,
    PRIMARY KEY (user_id, role_id)
);
GO

CREATE TABLE Permissions (
    perm_id  INT IDENTITY PRIMARY KEY,
    resource NVARCHAR(40),
    action   NVARCHAR(10),
    UNIQUE (resource, action)
);
GO

CREATE TABLE RolePerm (
    role_id INT REFERENCES Roles,
    perm_id INT REFERENCES Permissions,
    PRIMARY KEY (role_id, perm_id)
);
GO

CREATE TABLE Device (
    device_id     INT IDENTITY PRIMARY KEY,
    device_code   NVARCHAR(30) UNIQUE,
    location      NVARCHAR(80),
    firmware      NVARCHAR(20),
    status        NVARCHAR(20) DEFAULT 'ACTIVE',
    control_owner INT NULL REFERENCES Users,
    last_seen     DATETIME2 NULL,
    api_key_hash  CHAR(64) NOT NULL
);
GO

CREATE TABLE SensorType (
    sensor_type_id INT IDENTITY PRIMARY KEY,
    name           NVARCHAR(40),
    unit           NVARCHAR(20),
    min_valid      FLOAT,
    max_valid      FLOAT
);
GO

CREATE TABLE Calibration (
    calib_id       INT IDENTITY PRIMARY KEY,
    device_id      INT REFERENCES Device,
    sensor_type_id INT REFERENCES SensorType,
    offset_val     FLOAT,
    scale_val      FLOAT,
    version        INT,
    valid_until    DATE,
    created_by     INT REFERENCES Users
);
GO

CREATE TABLE OperatingProfile (
    profile_id  INT IDENTITY PRIMARY KEY,
    device_id   INT REFERENCES Device,
    params_json NVARCHAR(MAX),
    updated_by  INT REFERENCES Users,
    updated_at  DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE Alert (
    alert_id   INT IDENTITY PRIMARY KEY,
    device_id  INT REFERENCES Device,
    session_id BIGINT,
    source     NVARCHAR(10),
    label      NVARCHAR(40),
    risk_score FLOAT NULL,
    status     NVARCHAR(20) DEFAULT 'OPEN',
    created_at DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE AlertEvidence (
    evidence_id INT IDENTITY PRIMARY KEY,
    alert_id    INT REFERENCES Alert,
    item        NVARCHAR(60),
    value_text  NVARCHAR(200)
);
GO

CREATE TABLE MaintenanceTicket (
    ticket_id   INT IDENTITY PRIMARY KEY,
    alert_id    INT NULL REFERENCES Alert,
    device_id   INT REFERENCES Device,
    assigned_to INT REFERENCES Users,
    note        NVARCHAR(400),
    status      NVARCHAR(20) DEFAULT 'OPEN',
    opened_at   DATETIME2 DEFAULT SYSUTCDATETIME(),
    closed_at   DATETIME2 NULL
);
GO

CREATE TABLE AuditLog (
    audit_id      BIGINT IDENTITY PRIMARY KEY,
    actor_user_id INT,
    action        NVARCHAR(50),
    object_type   NVARCHAR(50),
    object_id     BIGINT,
    diff_json     NVARCHAR(MAX),
    ts            DATETIME2(7) DEFAULT SYSUTCDATETIME(),
    row_hash      CHAR(64) NULL
);
GO

CREATE TABLE HashChain (
    anchor_id  BIGINT IDENTITY PRIMARY KEY,
    audit_id   BIGINT REFERENCES AuditLog,
    prev_hash  CHAR(64),
    chain_hash CHAR(64)
);
GO

CREATE TABLE Store (
    store_id     INT IDENTITY PRIMARY KEY,
    store_code   NVARCHAR(20)  NOT NULL UNIQUE,
    store_name   NVARCHAR(100) NOT NULL,
    address      NVARCHAR(200) NULL,
    phone        NVARCHAR(20)  NULL,
    bank_account NVARCHAR(30)  NULL,
    status       NVARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE PaymentTransaction (
    txn_id           BIGINT IDENTITY PRIMARY KEY,
    store_id         INT NOT NULL REFERENCES Store,
    device_id        INT NOT NULL REFERENCES Device,
    sepay_txn_id     BIGINT NOT NULL,
    gateway          NVARCHAR(30) NULL,
    account_number   NVARCHAR(30) NULL,
    amount           DECIMAL(18,0) NOT NULL,
    transfer_content NVARCHAR(255) NULL,
    reference_code   NVARCHAR(60) NULL,
    measured_at      DATETIME2 NOT NULL,
    ingested_at      DATETIME2 DEFAULT SYSUTCDATETIME(),
    txn_min_of_day   INT NULL,
    expected_amount  DECIMAL(18,0) NULL,
    gap_seconds_prev FLOAT NULL,
    UNIQUE (device_id, sepay_txn_id)
);
GO

CREATE TABLE TransactionLabel (
    txn_id     BIGINT PRIMARY KEY REFERENCES PaymentTransaction,
    label      NVARCHAR(40) NOT NULL
               CONSTRAINT CK_TransactionLabel_label CHECK
               (label IN ('LEGIT', 'SUSPECTED_FRAUD', 'DUPLICATE', 'AMOUNT_MISMATCH', 'DEVICE_OFFLINE')),
    labeled_by INT NULL REFERENCES Users,
    labeled_at DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE NotificationLog (
    notify_id    BIGINT IDENTITY PRIMARY KEY,
    txn_id       BIGINT NOT NULL REFERENCES PaymentTransaction,
    device_id    INT NOT NULL REFERENCES Device,
    channel      NVARCHAR(10) NOT NULL
                 CONSTRAINT CK_NotificationLog_channel CHECK (channel IN ('SPEAKER', 'PRINTER')),
    message_text NVARCHAR(300) NULL,
    status       NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CONSTRAINT CK_NotificationLog_status CHECK (status IN ('PENDING', 'DONE', 'FAILED')),
    error_text   NVARCHAR(200) NULL,
    created_at   DATETIME2 DEFAULT SYSUTCDATETIME(),
    completed_at DATETIME2 NULL
);
GO

ALTER TABLE Users  ADD store_id INT NULL CONSTRAINT FK_Users_Store  REFERENCES Store;
ALTER TABLE Device ADD store_id INT NULL CONSTRAINT FK_Device_Store REFERENCES Store;
GO

CREATE TRIGGER trg_PaymentTransaction_AppendOnly ON PaymentTransaction
INSTEAD OF UPDATE, DELETE AS
BEGIN
    RAISERROR(N'Payment transaction records are append-only', 16, 1);
END;
GO

CREATE VIEW v_PaymentTransaction_Features AS
SELECT t.txn_id, t.store_id, t.device_id, t.amount, t.txn_min_of_day,
       t.expected_amount, t.gap_seconds_prev, l.label
FROM PaymentTransaction t
JOIN TransactionLabel l ON l.txn_id = t.txn_id;
GO

CREATE INDEX IX_Users_store       ON Users(store_id);
CREATE INDEX IX_UserRole_role     ON UserRole(role_id);
CREATE INDEX IX_Device_store      ON Device(store_id);
CREATE INDEX IX_AuditLog_ts       ON AuditLog(ts DESC);
CREATE INDEX IX_AuditLog_action   ON AuditLog(action);
CREATE INDEX IX_Txn_store_time    ON PaymentTransaction(store_id, measured_at DESC);
GO

CREATE OR ALTER PROCEDURE Audit_Append
    @actor INT, @action NVARCHAR(50), @objectType NVARCHAR(50), @objectId BIGINT, @diff NVARCHAR(MAX)
AS
BEGIN
    SET XACT_ABORT ON;
    BEGIN TRAN;
    DECLARE @id BIGINT, @ts DATETIME2(7), @prev CHAR(64), @row NVARCHAR(MAX), @hash CHAR(64);

    INSERT INTO AuditLog (actor_user_id, action, object_type, object_id, diff_json)
    VALUES (@actor, @action, @objectType, @objectId, @diff);
    SET @id = SCOPE_IDENTITY();
    SELECT @ts = ts FROM AuditLog WHERE audit_id = @id;

    SELECT TOP 1 @prev = chain_hash FROM HashChain WITH (UPDLOCK, HOLDLOCK) ORDER BY anchor_id DESC;
    SET @prev = ISNULL(@prev, REPLICATE('0', 64));

    SET @row = CONCAT(@id, '|', CONVERT(NVARCHAR(33), @ts, 126), '|', @actor, '|', @action, '|',
                      @objectType, '|', @objectId, '|', ISNULL(@diff, ''), '|', @prev);
    SET @hash = LOWER(CONVERT(CHAR(64), HASHBYTES('SHA2_256', @row), 2));

    UPDATE AuditLog SET row_hash = @hash WHERE audit_id = @id;
    INSERT INTO HashChain (audit_id, prev_hash, chain_hash) VALUES (@id, @prev, @hash);
    COMMIT;
END;
GO
