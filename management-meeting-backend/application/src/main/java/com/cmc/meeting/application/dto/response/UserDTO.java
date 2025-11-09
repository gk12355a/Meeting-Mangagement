package com.cmc.meeting.application.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) đại diện cho thông tin User cơ bản, an toàn.
 * Dùng để trả về trong các API (ví dụ: chi tiết cuộc họp, kết quả tìm kiếm người dùng)
 * mà không làm lộ thông tin nhạy cảm (như password, roles, status).
 */
@Data
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String fullName;

    /**
     * Thêm 'username' (thường là email)
     * để frontend có thể hiển thị rõ ràng khi tìm kiếm
     * Ví dụ: "Nguyen Van A (anv@company.com)"
     */
    private String username;
}