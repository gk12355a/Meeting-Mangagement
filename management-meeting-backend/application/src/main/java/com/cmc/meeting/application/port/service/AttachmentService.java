package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.attachment.AttachmentDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface AttachmentService {
    AttachmentDTO uploadAttachment(Long meetingId, Long currentUserId, MultipartFile file) throws IOException;
    void deleteAttachment(Long attachmentId, Long currentUserId) throws IOException;
}