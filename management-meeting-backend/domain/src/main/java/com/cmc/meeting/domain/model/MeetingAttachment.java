package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MeetingAttachment {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String filePublicId; // Dùng để xóa trên Cloudinary

    // Quan hệ
    private Meeting meeting;

    public MeetingAttachment(String fileName, String fileUrl, String filePublicId, Meeting meeting) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.filePublicId = filePublicId;
        this.meeting = meeting;
    }
}