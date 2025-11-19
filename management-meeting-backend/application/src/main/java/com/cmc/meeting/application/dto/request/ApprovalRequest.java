package com.cmc.meeting.application.dto.request;

import lombok.Data;

@Data
public class ApprovalRequest {
    private boolean approved; // true = Đồng ý, false = Từ chối
    private String reason;    // Lý do (bắt buộc nếu từ chối)
}