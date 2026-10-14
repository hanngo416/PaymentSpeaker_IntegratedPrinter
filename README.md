# Tinky (PaymentSpeaker)

Hệ thống hỗ trợ thanh toán cho cửa hàng nhỏ: thu ngân tạo đơn kèm mã VietQR, hệ thống tự nhận giao dịch từ ngân hàng qua SePay, đối chiếu với đơn, phát hiện giao dịch đáng ngờ và đọc thông báo tiền về bằng giọng nói. Làm bằng JSP/Servlet theo mô hình MVC, SQL Server, không dùng framework.

## Yêu cầu

- NetBeans, JDK 8, Tomcat 9 (chạy ở đường dẫn gốc `/`, xem `web/META-INF/context.xml`)
- SQL Server, bật TCP/IP cổng 1433, đăng nhập `sa`
- Thư viện đã có sẵn trong `web/WEB-INF/lib`: `sqljdbc4.jar`, `jstl-1.2.jar`, `javax.mail-1.6.2.jar`, `gson-2.8.9.jar`

## Chạy lần đầu

1. Mở SQL Server Management Studio, chạy lần lượt `sql/01_schema.sql`, `sql/02_seed.sql`, `sql/03_checks.sql`.
2. Ở `03_checks.sql`, cột `row_count` phải bằng cột `expected` (6 người dùng, 2 thiết bị, 37 đơn, 38 giao dịch, 8 cảnh báo, 1 dòng audit), và có đúng 9 khóa ngoại.
3. Mở `src/java/util/DBUtil.java`, sửa máy chủ, tài khoản và mật khẩu cho khớp máy mình. Đây là chỗ duy nhất giữ chuỗi kết nối.
4. Trong `src/java`, chép từng file `.example` thành file cùng tên không có đuôi `.example` rồi điền giá trị:
   - `mail.properties`: Gmail và mật khẩu ứng dụng, dùng để gửi mã OTP
   - `google-oauth.properties`: client id, client secret, redirect uri, dùng cho đăng nhập Google
   - `sepay.properties`: token SePay và số tài khoản, dùng để nhận giao dịch thật
5. Trong NetBeans, chuột phải vào dự án, chọn Run.
6. Mở `/health` trước tiên để biết kết nối cơ sở dữ liệu có ổn không. Nếu báo lỗi thì dừng ở đó, gần như mọi lỗi lần đầu nằm ở tên máy chủ, cổng 1433 chưa bật hoặc sai mật khẩu `sa`.
7. Mở `/login`, nhập email của một tài khoản mẫu bên dưới, lấy mã OTP trong hộp thư.

Nếu chưa điền `mail.properties` thì không nhận được OTP. Nếu chưa điền `sepay.properties` thì không nhận được giao dịch thật, nhưng vẫn xem được 38 giao dịch mẫu.

## Tài khoản mẫu

Hệ thống không lưu mật khẩu. Nhân viên đăng nhập bằng mã OTP gửi qua email (mã được băm kèm salt trong bảng `LoginOtps`) hoặc bằng tài khoản Google. Năm tài khoản dưới đây dùng địa chỉ dạng `+alias` của cùng một hộp thư để một người nhận được OTP của mọi vai trò.

| Tài khoản | Email | Vai trò | Thấy được |
|---|---|---|---|
| `han_admin` | `hanngo816@gmail.com` | ADMIN | Mọi chức năng |
| `mai_manager` | `hanngo816+manager@gmail.com` | STORE_MANAGER | Người dùng (chỉ thu ngân), giao dịch, cảnh báo, loa |
| `nam_cashier` | `hanngo816+cashier@gmail.com` | CASHIER | Tạo đơn, giao dịch, loa |
| `dung_auditor` | `hanngo816+auditor@gmail.com` | AUDITOR | Giao dịch, xử lý cảnh báo |
| `em_technician` | `hanngo816+tech@gmail.com` | TECHNICIAN | Trang chủ, hồ sơ |
| `ha_cashier` | `hanngo816+locked@gmail.com` | CASHIER, đang bị khóa | Không đăng nhập được |

## Cơ sở dữ liệu

Chín bảng, chín khóa ngoại, tạo một lượt từ trên xuống, không cần câu `ALTER TABLE`:

`Roles` → `Users` → `LoginOtps` → `Devices` → `MaintenanceTickets` → `Orders` → `Transactions` → `FraudAlerts` → `AuditLogs`

- `01_schema.sql` chỉ tạo bảng khi chưa có, chạy lại không mất dữ liệu.
- `02_seed.sql` bỏ qua nếu đã có người dùng hoặc giao dịch.
- `99_reset_demo.sql` xóa sạch dữ liệu để dựng lại demo. Chỉ chạy khi chủ động muốn reset. Thứ tự sau reset: `99_reset_demo.sql`, `01_schema.sql`, `02_seed.sql`.

## Nguồn dữ liệu

- Dữ liệu mẫu (38 giao dịch, 37 đơn, 8 cảnh báo) do nhóm tự dựng trong `02_seed.sql`, không lấy từ nguồn công khai.
- Giao dịch thật được kéo từ API SePay (tài khoản của nhóm) mỗi 60 giây trong cửa sổ 30 phút gần nhất. Mỗi giao dịch có mã ngân hàng duy nhất nên kéo lại không ghi trùng.
- Ngày lấy dữ liệu thật: ghi vào đây khi quay video và nộp bài: ______

## Thử với bảng lớn

Để thử trang giao dịch với nhiều dòng, dùng công cụ sinh dữ liệu (chạy từ thư mục dự án):

```
javac -cp web/WEB-INF/lib/sqljdbc4.jar -d build/tools src/java/util/DBUtil.java tools/GenerateTransactions.java
java -cp "build/tools;web/WEB-INF/lib/sqljdbc4.jar" GenerateTransactions 20000
java -cp "build/tools;web/WEB-INF/lib/sqljdbc4.jar" GenerateTransactions clear
```

Lệnh thứ hai thêm 20.000 giao dịch trải đều 90 ngày gần nhất (mã bắt đầu bằng `GEN`, đã đánh dấu là đã đọc nên loa không phát). Lệnh thứ ba xóa chúng đi. Nhớ chạy lệnh xóa trước khi quay demo.

## Thư mục

| Thư mục | Nội dung |
|---|---|
| `src/java/controller` | Servlet, chia theo `auth`, `admin`, `order`, `speaker`, `printer`, `transaction`, `dashboard`, `system` |
| `src/java/service` | Nghiệp vụ: OTP, mail, Google, SePay, nhận giao dịch, người dùng, đơn hàng |
| `src/java/dao` | Toàn bộ câu SQL, dùng `PreparedStatement` |
| `src/java/rules` | Bộ luật phát hiện giao dịch đáng ngờ |
| `src/java/filter` | `AuthFilter` và bảng phân quyền theo vai trò |
| `web/WEB-INF/views` | Trang JSP, chỉ nhận dữ liệu đã chuẩn bị |
| `web/assets` | CSS và JavaScript, không dùng thư viện ngoài |
| `sql` | Các file SQL |
| `tools` | Công cụ sinh dữ liệu |
| `test-tools` | `RuleTest.java` kiểm tra bộ luật |
| `docs` | Bài giảng, kịch bản video (kèm câu hỏi bảo vệ), bảng test case |

## Chưa làm

- Trang quản lý thiết bị cho kỹ thuật viên
- In biên lai bằng máy in nhiệt (trang `/printer` chỉ báo "Coming soon")
- Loa phần cứng (hiện dùng giọng đọc có sẵn của trình duyệt)
