package com.cmc.meeting.application.port.security;

import java.util.Set;

// Đây là "Hợp đồng" (Port) mà Application Layer định nghĩa
public interface TokenProvider {

    // Định nghĩa các hàm mà AuthService cần

    // (Chúng ta có thể thêm getUserIdFromToken, getUsernameFromToken...
    // vào đây sau này nếu các service khác cần)
    String generateToken(Long userId, String username, Set<String> roles);
}