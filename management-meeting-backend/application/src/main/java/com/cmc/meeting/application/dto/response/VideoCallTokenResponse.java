package com.cmc.meeting.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoCallTokenResponse {
    private String token;
    private String apiKey;
    private UserDetailDTO user; // Class con nếu cần

    @Data
    @AllArgsConstructor
    public static class UserDetailDTO {
        private String id;
        private String name;
        private String image;
    }
}