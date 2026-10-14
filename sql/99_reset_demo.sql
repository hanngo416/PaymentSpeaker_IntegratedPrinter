-- =====================================================================================
-- RESET DEMO - DANGER: DELETES ALL DATA (users, orders, real transactions, audit logs)
-- Only run this when you really want a clean database for a demo.
-- Order after reset: 99_reset_demo.sql -> 01_schema.sql -> 02_seed.sql
-- Back up first: BACKUP DATABASE PaymentSpeaker TO DISK = 'D:\backup\ps.bak'
-- =====================================================================================
USE PaymentSpeaker;
GO

-- Drop existing tables (children first, then parents)
IF OBJECT_ID('AuditLogs', 'U') IS NOT NULL DROP TABLE AuditLogs;
IF OBJECT_ID('Refunds', 'U') IS NOT NULL DROP TABLE Refunds;
IF OBJECT_ID('FraudAlerts', 'U') IS NOT NULL DROP TABLE FraudAlerts;
IF OBJECT_ID('Transactions', 'U') IS NOT NULL DROP TABLE Transactions;
IF OBJECT_ID('Orders', 'U') IS NOT NULL DROP TABLE Orders;
IF OBJECT_ID('MaintenanceTickets', 'U') IS NOT NULL DROP TABLE MaintenanceTickets;
IF OBJECT_ID('Devices', 'U') IS NOT NULL DROP TABLE Devices;
IF OBJECT_ID('LoginOtps', 'U') IS NOT NULL DROP TABLE LoginOtps;
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
IF OBJECT_ID('Roles', 'U') IS NOT NULL DROP TABLE Roles;
IF OBJECT_ID('Stores', 'U') IS NOT NULL DROP TABLE Stores;
GO
