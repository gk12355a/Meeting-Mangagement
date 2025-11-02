package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataMeetingRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;
import java.util.List; // Bổ sung
import java.util.stream.Collectors; // Bổ sung
import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public class MeetingRepositoryAdapter implements MeetingRepository {

    private final SpringDataMeetingRepository jpaRepository;
    private final ModelMapper modelMapper;

    public MeetingRepositoryAdapter(SpringDataMeetingRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Meeting save(Meeting meeting) {
        // Map từ Domain Model -> Entity
        MeetingEntity entity = modelMapper.map(meeting, MeetingEntity.class);

        // Lưu Entity
        MeetingEntity savedEntity = jpaRepository.save(entity);

        // Map ngược từ Entity -> Domain Model để trả về
        return modelMapper.map(savedEntity, Meeting.class);
    }

    @Override
    public Optional<Meeting> findById(Long id) {
        return jpaRepository.findById(id)
                .map(entity -> modelMapper.map(entity, Meeting.class));
    }

    @Override
    public boolean isRoomBusy(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        // Gọi query tùy chỉnh mà chúng ta đã viết
        return jpaRepository.findRoomOverlap(roomId, startTime, endTime);
    }
    @Override
    public List<Meeting> findAllByUserId(Long userId) {
        // 1. Gọi query JPA
        List<MeetingEntity> entities = jpaRepository.findAllByUserId(userId);

        // 2. Map List<Entity> sang List<Domain Model>
        return entities.stream()
                .map(entity -> modelMapper.map(entity, Meeting.class))
                .collect(Collectors.toList());
    }
}