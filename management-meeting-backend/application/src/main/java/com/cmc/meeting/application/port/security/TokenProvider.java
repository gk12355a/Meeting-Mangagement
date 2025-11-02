package com.cmc.meeting.application.port.security;

// Đây là "Hợp đồng" (Port) mà Application Layer định nghĩa
public interface TokenProvider {

    // Định nghĩa các hàm mà AuthService cần
    String generateToken(Long userId, String username);

    // (Chúng ta có thể thêm getUserIdFromToken, getUsernameFromToken...
    // vào đây sau này nếu các service khác cần)
}