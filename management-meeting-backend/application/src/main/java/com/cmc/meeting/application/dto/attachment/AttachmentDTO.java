package com.cmc.meeting.application.dto.attachment;

import lombok.Data;

@Data
public class AttachmentDTO {
    private Long id;
    private String fileName;
    private String fileUrl;
}