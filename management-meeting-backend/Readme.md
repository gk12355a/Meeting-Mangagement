# 🧩 Meeting Room Backend — Test Plan

Chào bạn 👋  

Đây là **Kế hoạch Kiểm thử (Test Plan)** chi tiết cho toàn bộ hệ thống **Meeting Room Backend**.  
Tài liệu này tổng hợp tất cả **User Story**, **API**, **dữ liệu mẫu**, và **kết quả mong đợi** để bạn có thể dễ dàng thực hiện test thủ công hoặc tự động (Postman/Newman/Swagger).

---

## ⚙️ Setup (Thiết lập môi trường)

Trước khi tiến hành test, hãy đảm bảo bạn đã thực hiện đúng các bước sau:

1. **Reset cơ sở dữ liệu test**
   - Thực hiện `DROP DATABASE test; CREATE DATABASE test;`
   - Đảm bảo `spring.jpa.hibernate.ddl-auto=update` để tự động tạo bảng mới nhất.
   - File `data.sql` sẽ được chạy để mồi dữ liệu ban đầu.

2. **Khởi động ứng dụng**
   - Chạy file `MeetingRoomApplication.java`.

---

## 👥 Tài khoản Test (Token Test)

Lấy token qua API: `POST /auth/login`.

| Vai trò | Username | Password | ID | Ghi chú |
|----------|-----------|-----------|-----|----------|
| **User thường** | `kiendotri@cmc.com` | `nguyenlee24` | `3` | Dùng cho test User Story của người dùng |
| **Admin (ROLE_ADMIN + ROLE_VIP)** | `admin@cmc.com` | `123456` | `4` | Dùng cho test quyền quản trị |

---

## 🧪 Kế hoạch Kiểm thử Chi tiết

### 1️⃣ Epic 5: Xác thực & Quên Mật khẩu

| User Story | API | Token | Body | Kết quả Mong đợi |
|-------------|------|--------|------|------------------|
| **US-18** | `POST /api/v1/auth/register` | ❌ | `{"username": "user.moi@cmc.com", "password": "password123", "fullName": "User Moi"}` | ✅ `200 OK` — “Đăng ký thành công” |
| **US-18** | `POST /api/v1/auth/login` | ❌ | `{"username": "kiendotri@cmc.com", "password": "nguyenlee24"}` | ✅ `200 OK` — Trả về `accessToken` |
| **BS-5.1** | `POST /api/v1/auth/forgot-password` | ❌ | `{"email": "kiendotri@cmc.com"}` | ✅ `200 OK` — “Nếu email tồn tại…” |
| **BS-5.3** | `POST /api/v1/auth/reset-password` | ❌ | `{"token": "[Token_tu_email]", "newPassword": "newPassword123"}` | ✅ `200 OK` — “Đặt lại mật khẩu thành công” |
| ❌ **Lỗi 401** | `POST /api/v1/auth/login` | ❌ | `{"username": "kiendotri@cmc.com", "password": "SAI"}` | ❌ `401 Unauthorized` — “Sai tên hoặc mật khẩu” |

---

### 2️⃣ Epic 2, 3, 5: Quản trị (Admin)

| User Story | API | Token | Body / Params | Kết quả Mong đợi |
|-------------|------|--------|---------------|------------------|
| ❌ **Lỗi 403** | `GET /api/v1/admin/users` | `TOKEN_USER` | - | ❌ `403 Forbidden` |
| **US-19** | `GET /api/v1/admin/users` | `TOKEN_ADMIN` | - | ✅ `200 OK` — Danh sách user |
| **US-18** | `PUT /api/v1/admin/users/3` | `TOKEN_ADMIN` | `{"roles": ["ROLE_USER"], "isActive": false}` | ✅ `200 OK` — User bị vô hiệu hóa |
| **US-11** | `POST /api/v1/rooms` | `TOKEN_ADMIN` | `{"name": "Phòng VIP 1", "capacity": 5, "status": "AVAILABLE", "requiredRoles": ["ROLE_VIP"]}` | ✅ `201 Created` |
| **BS-11.1** | `PUT /api/v1/rooms/2` | `TOKEN_ADMIN` | `{"status": "UNDER_MAINTENANCE"}` | ✅ `200 OK` |
| **US-14** | `POST /api/v1/devices` | `TOKEN_ADMIN` | `{"name": "Máy chiếu 01", "description": "Hàng xịn", "status": "AVAILABLE"}` | ✅ `201 Created` |

---

### 3️⃣ Epic 1 & 3: Luồng Họp Cơ bản (User)

Giả định:  
- Phòng 1 = public  
- Phòng 2 = VIP  
- Thiết bị 1 = Máy chiếu 01  

| User Story | API | Token | Body | Kết quả Mong đợi |
|-------------|------|--------|------|------------------|
| ❌ **403 (US-21)** | `POST /api/v1/meetings` | `TOKEN_USER` | Phòng VIP | ❌ `403 Forbidden` — Không đủ quyền |
| ❌ **403 (BS-11.1)** | `POST /api/v1/meetings` | `TOKEN_ADMIN` | Phòng bảo trì | ❌ `403 Forbidden` |
| **US-1, 4, 12, BS-20.1, BS-29** | `POST /api/v1/meetings` | `TOKEN_USER` | Tạo cuộc họp đầy đủ thông tin | ✅ `201 Created` — Ghi lại `id` |
| **US-6** | `GET /api/v1/meetings/my-meetings` | `TOKEN_ADMIN` | - | ✅ `200 OK` — Có “Họp Tổng hợp” |
| **BS-1.2** | `GET /api/v1/meetings/1` | `TOKEN_ADMIN` | - | ✅ `200 OK` |
| **US-2** | `PUT /api/v1/meetings/1` | `TOKEN_ADMIN` | Sửa tiêu đề | ✅ `200 OK` |
| **BS-4.1** | `POST /api/v1/attachments/upload/1` | `TOKEN_ADMIN` | Form-Data: file | ✅ `201 Created` — Có `fileUrl` |
| **BS-4.1** | `DELETE /api/v1/attachments/1` | `TOKEN_ADMIN` | - | ✅ `200 OK` |
| **US-23** | `DELETE /api/v1/meetings/1` | `TOKEN_ADMIN` | `{"reason": "Test hủy đơn"}` | ✅ `200 OK` — “Đã hủy...” |

---

### 4️⃣ Epic 1: Lịch định kỳ & Phản hồi

| User Story | API | Token | Body / Params | Kết quả Mong đợi |
|-------------|------|--------|---------------|------------------|
| **US-3** | `POST /api/v1/meetings` | `TOKEN_USER` | Lặp lại hàng tuần | ✅ `201 Created` — Tạo nhiều họp con |
| **BS-1.1** | `POST /api/v1/meetings/{id}/respond` | Token User 2 | `{"status": "ACCEPTED"}` | ✅ `200 OK` |
| **BS-2.1** | `DELETE /api/v1/meetings/series/{seriesId}` | `TOKEN_USER` | `{"reason": "Hủy cả chuỗi"}` | ✅ `200 OK` |
| **BS-1.1** | `GET /api/v1/meetings/respond-by-link` | ❌ | `token=...&status=DECLINED` | ✅ `200 OK` (HTML) |

---

### 5️⃣ Epic 7: Tính năng Nâng cao (Gợi ý & Check-in)

| User Story | API | Token | Body / Params | Kết quả Mong đợi |
|-------------|------|--------|---------------|------------------|
| **US-26** | `GET /api/v1/rooms/available` | `TOKEN_USER` | `startTime`, `endTime`, `capacity` | ✅ `200 OK` |
| **US-5** | `POST /api/v1/meetings/suggest-time` | `TOKEN_USER` | `participantIds`, `rangeStart`, `rangeEnd`, `durationMinutes` | ✅ `200 OK` |
| **US-27** | `POST /api/v1/meetings` | `TOKEN_USER` | Họp tức thì | ✅ `201 Created` |
| **US-27** | `POST /api/v1/meetings/check-in` | `TOKEN_USER` | `{"roomId": 1}` | ✅ `200 OK` — “Check-in thành công” |

---

### 6️⃣ Epic 6: Báo cáo & Excel (Admin)

| User Story | API | Token | Params | Kết quả Mong đợi |
|-------------|------|--------|---------|------------------|
| **US-22** | `GET /api/v1/reports/room-usage` | `TOKEN_ADMIN` | `from`, `to` | ✅ `200 OK` — JSON thống kê |
| **US-24** | `GET /api/v1/reports/room-usage?format=excel` | `TOKEN_ADMIN` | `from`, `to` | ✅ `200 OK` — File Excel tải về |
| **US-23** | `GET /api/v1/reports/cancelation-stats` | `TOKEN_ADMIN` | `from`, `to` | ✅ `200 OK` |
| **BS-31** | `GET /api/v1/reports/visitors` | `TOKEN_ADMIN` | `date` | ✅ `200 OK` |

---

### 7️⃣ Epic 5: Nhóm Liên hệ (User)

| User Story | API | Token | Body | Kết quả Mong đợi |
|-------------|------|--------|------|------------------|
| **BS-20.3** | `POST /api/v1/contact-groups` | `TOKEN_USER` | `{"name": "Team Dev", "memberIds": [2, 4]}` | ✅ `201 Created` |
| **BS-20.3** | `GET /api/v1/contact-groups` | `TOKEN_USER` | - | ✅ `200 OK` — Có “Team Dev” |
| **BS-20.3** | `PUT /api/v1/contact-groups/1` | `TOKEN_USER` | `{"name": "Team Dev Mới", "memberIds": [2]}` | ✅ `200 OK` |
| **BS-20.3** | `DELETE /api/v1/contact-groups/1` | `TOKEN_USER` | - | ✅ `200 OK` — “Đã xóa” |

---

## 📦 Ghi chú

- Bạn có thể import toàn bộ các test case này vào **Postman** hoặc **Newman CLI**.  
- Đề xuất tạo **Collection** theo Epic để test song song hoặc CI/CD Pipeline.  
- Tất cả `TOKEN` cần được thay bằng token thật từ API `/auth/login`.

---

## 🏁 Kết luận

Bộ **Test Plan** này bao phủ toàn bộ hệ thống backend từ xác thực, quản trị, cuộc họp, nhóm liên hệ, đến báo cáo.  
Hãy chạy tuần tự theo thứ tự Epic để đảm bảo dữ liệu nhất quán và kết quả chính xác nhất.

> 💡 Nếu cần, bạn có thể tạo thêm file `postman_collection.json` để tự động hóa toàn bộ quy trình test này.

mvn clean install -DskipTests 
java -jar web/target/web-1.0.0-SNAPSHOT.jar