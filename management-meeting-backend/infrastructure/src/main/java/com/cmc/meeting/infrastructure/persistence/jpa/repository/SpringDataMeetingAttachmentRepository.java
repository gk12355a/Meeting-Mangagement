package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMeetingAttachmentRepository extends JpaRepository<MeetingAttachmentEntity, Long> {
}