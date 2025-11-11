package com.cmc.meeting.application.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
// (Bạn có thể thêm validation nếu cần)
// import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class UserProfileUpdateRequest {

    // @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    // (Trong tương lai, bạn có thể thêm các trường khác ở đây
    //  ví dụ: phoneNumber, title, ...)
}