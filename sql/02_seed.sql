-- Dữ liệu mẫu. Chạy SAU 01_schema.sql
USE PaymentSpeaker;
GO

-- =====================================================================================
-- 4. DỮ LIỆU KHỞI TẠO MẪU (SEED DATA ĐẦY ĐỦ NGHIỆP VỤ)
-- =====================================================================================

-- 4.1. Khởi tạo Roles
INSERT INTO Roles (role_id, role_name, description) VALUES
(1, N'ADMIN', N'Quản trị viên toàn hệ thống, toàn quyền cấu hình và quản lý nhân sự'),
(2, N'STORE_MANAGER', N'Cửa hàng trưởng, quản lý doanh thu và nhân sự chi nhánh'),
(3, N'CASHIER', N'Thu ngân tại quầy, tạo đơn hàng, nghe loa đọc tiền và in hóa đơn'),
(4, N'AUDITOR', N'Kiểm toán viên tra soát giao dịch gian lận và kiểm tra tính toàn vẹn dữ liệu'),
(5, N'TECHNICIAN', N'Kỹ thuật viên bảo trì phần cứng loa và máy in');
GO

-- 4.2. Khởi tạo Stores
INSERT INTO Stores (store_code, store_name, address, phone, status) VALUES
(N'STORE_HN01', N'Chi nhánh Hà Nội - Trụ sở Hoàn Kiếm', N'Số 12 Tràng Tiền, Hoàn Kiếm, Hà Nội', N'02439123456', N'ACTIVE'),
(N'STORE_SG01', N'Chi nhánh TP.HCM - Quầy Bến Thành', N'Số 88 Lê Lợi, Bến Nghé, Quận 1, TP.HCM', N'02838999888', N'ACTIVE');
GO

-- 4.3. Khởi tạo Users
-- CHÚ Ý: password_hash dưới đây chỉ là GIÁ TRỊ GIỮ CHỖ, KHÔNG phải hash của "123456".
-- Bài 2 sẽ tạo lại mật khẩu bằng PBKDF2 (script 03_seed_passwords.sql).
INSERT INTO Users (username, password_hash, salt, full_name, role_id, store_id, status) VALUES
(N'admin', N'd82494f05d6917ba02f7aaa29689ccb444bb73f20380876cb05d1f37537b7892', N'salt_demo', N'Nguyễn Quản Trị', 1, NULL, N'ACTIVE'),
(N'manager_hn', N'd82494f05d6917ba02f7aaa29689ccb444bb73f20380876cb05d1f37537b7892', N'salt_demo', N'Trần Trưởng Quán', 2, 1, N'ACTIVE'),
(N'cashier_hn', N'd82494f05d6917ba02f7aaa29689ccb444bb73f20380876cb05d1f37537b7892', N'salt_demo', N'Lê Thu Ngân', 3, 1, N'ACTIVE'),
(N'auditor', N'd82494f05d6917ba02f7aaa29689ccb444bb73f20380876cb05d1f37537b7892', N'salt_demo', N'Phạm Kiểm Toán', 4, NULL, N'ACTIVE'),
(N'technician', N'd82494f05d6917ba02f7aaa29689ccb444bb73f20380876cb05d1f37537b7892', N'salt_demo', N'Hoàng Kỹ Thuật', 5, NULL, N'ACTIVE');
GO

-- 4.4. Khởi tạo Devices
INSERT INTO Devices (device_code, device_type, model_name, store_id, api_key, status) VALUES
(N'SPK_HN01', N'SPEAKER', N'Smart Voice Box Pro V2', 1, N'KEY_SPK_HN01_SECRET_9876', N'ONLINE'),
(N'PRN_HN01', N'PRINTER', N'Aiyin A70 Thermal 80mm', 1, N'KEY_PRN_HN01_SECRET_5432', N'ONLINE'),
(N'SPK_SG01', N'SPEAKER', N'Smart Voice Box Mini', 2, N'KEY_SPK_SG01_SECRET_1122', N'ONLINE');
GO

-- 4.5. Khởi tạo Đơn hàng mẫu (Orders)
-- Đơn 1: Chờ thanh toán hợp lệ
INSERT INTO Orders (order_code, store_id, created_by, amount, status, qr_content, expires_at) VALUES
(N'TINKY10234', 1, 3, 50000.00, N'PENDING', N'2|99|0987654321|NGUYEN VAN A||0|0|50000|TINKY10234|', DATEADD(MINUTE, 15, SYSDATETIME())),
-- Đơn 2: Chờ thanh toán để test chuyển thiếu tiền
(N'TINKY10235', 1, 3, 120000.00, N'PENDING', N'2|99|0987654321|NGUYEN VAN A||0|0|120000|TINKY10235|', DATEADD(MINUTE, 15, SYSDATETIME()));
GO

-- 4.6. Khởi tạo Genesis Block cho AuditLogs (Khối khởi nguyên của chuỗi băm)
-- Khối đầu tiên luôn có prev_hash gồm 64 ký tự '0'
INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, prev_hash, curr_hash, created_at) VALUES
(N'SYSTEM', N'INIT_SYSTEM', N'SYSTEM', N'0', N'{"message": "Genesis block created. System initialized."}', 
 '0000000000000000000000000000000000000000000000000000000000000000', 
 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 
 SYSDATETIME());
GO

PRINT N'>>> Hoàn tất khởi tạo toàn bộ CSDL và dữ liệu mẫu cho đồ án PaymentSpeaker!';
GO
