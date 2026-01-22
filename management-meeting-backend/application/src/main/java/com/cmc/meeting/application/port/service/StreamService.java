package com.cmc.meeting.application.port.service;


public interface StreamService {

    /**
     * Tạo Token truy cập Video Call cho User cụ thể.
     * Token này sẽ được Frontend sử dụng để kết nối với Stream Client.
     *
     * @param userId ID của người dùng (String)
     * @return Chuỗi JWT Token đã được ký
     */
    String createToken(String userId);

    /**
     * Lấy API Key công khai để trả về cho Client.
     *
     * @return API Key
     */
    String getApiKey();
}