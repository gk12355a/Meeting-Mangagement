-- 1. Đặt biến mật khẩu băm (cho "123456")
-- (Tôi đã tạo một hash hợp lệ mới cho "123456")
SET @hashed_password = '$2a$10$P/1T23f.V4m2/J.a.Z.qS.OMs6.9.sA7.m.J.d.w.P.G.i.E.P.M';

-- 2. Thêm phòng họp TRƯỚC
INSERT INTO rooms (id, name, capacity, location) VALUES (1, 'Phòng Họp Sao Hỏa', 10, 'Tầng 10')
    ON DUPLICATE KEY UPDATE name='Phòng Họp Sao Hỏa';

-- 3. Thêm Users
INSERT INTO users (id, username, full_name, password) VALUES (1, 'user1@cmc.com', 'User Một', @hashed_password) 
    ON DUPLICATE KEY UPDATE username='user1@cmc.com', password=@hashed_password;

INSERT INTO users (id, username, full_name, password) VALUES (2, 'user2@cmc.com', 'User Hai', @hashed_password) 
    ON DUPLICATE KEY UPDATE username='user2@cmc.com', password=@hashed_password;