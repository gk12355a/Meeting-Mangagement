package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.MeetingAttachment;
import com.cmc.meeting.domain.port.repository.MeetingAttachmentRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingAttachmentEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataMeetingAttachmentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MeetingAttachmentRepositoryAdapter implements MeetingAttachmentRepository {

    private final SpringDataMeetingAttachmentRepository jpaRepository;
    private final ModelMapper modelMapper;

    public MeetingAttachmentRepositoryAdapter(SpringDataMeetingAttachmentRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public MeetingAttachment save(MeetingAttachment attachment) {
        MeetingAttachmentEntity entity = modelMapper.map(attachment, MeetingAttachmentEntity.class);
        MeetingAttachmentEntity savedEntity = jpaRepository.save(entity);
        return modelMapper.map(savedEntity, MeetingAttachment.class);
    }

    @Override
    public Optional<MeetingAttachment> findById(Long id) {
        return jpaRepository.findById(id)
                .map(entity -> modelMapper.map(entity, MeetingAttachment.class));
    }

    @Override
    public void delete(MeetingAttachment attachment) {
        MeetingAttachmentEntity entity = modelMapper.map(attachment, MeetingAttachmentEntity.class);
        jpaRepository.delete(entity);
    }
}