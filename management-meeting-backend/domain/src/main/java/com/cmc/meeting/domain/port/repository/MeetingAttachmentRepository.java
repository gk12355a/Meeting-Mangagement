package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.MeetingAttachment;
import java.util.Optional;

public interface MeetingAttachmentRepository {
    MeetingAttachment save(MeetingAttachment attachment);
    Optional<MeetingAttachment> findById(Long id);
    void delete(MeetingAttachment attachment);
}