package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Device;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.DeviceEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.embeddable.EmbeddableParticipant;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataMeetingRepository;
import jakarta.annotation.PostConstruct;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MeetingRepositoryAdapter implements MeetingRepository {

    private final SpringDataMeetingRepository jpaRepository; // Tên biến chuẩn
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public MeetingRepositoryAdapter(SpringDataMeetingRepository jpaRepository,
                                    ModelMapper modelMapper,
                                    UserRepository userRepository) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
    }

    @Override
    public Meeting save(Meeting meeting) {
        MeetingEntity entity = toEntity(meeting);
        MeetingEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Meeting> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean isRoomBusy(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore) {
        return jpaRepository.findRoomOverlap(roomId, startTime, endTime, meetingIdToIgnore);
    }

    @Override
    public List<Meeting> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Meeting> findConfirmedMeetingsInDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findConfirmedMeetingsInDateRange(from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // --- CÁC HÀM HELPER ---

    private Meeting toDomain(MeetingEntity entity) {
        if (entity == null) return null;

        // 1. Map các trường đơn giản (Title, Time, Room...)
        Meeting meeting = modelMapper.map(entity, Meeting.class);

        // 2. Map thủ công 'participants' (Vì ModelMapper đã skip trong @PostConstruct)
        if (entity.getParticipants() != null) {
            Set<MeetingParticipant> participants = entity.getParticipants().stream()
                    .map(embeddable -> {
                        User user = userRepository.findById(embeddable.getUserId()).orElse(null);
                        if (user == null) return null;
                        return new MeetingParticipant(user, embeddable.getStatus(), embeddable.getResponseToken());
                    })
                    .filter(p -> p != null)
                    .collect(Collectors.toSet());
            meeting.setParticipants(participants);
        }

        return meeting;
    }

    private MeetingEntity toEntity(Meeting meeting) {
        if (meeting == null) return null;
        MeetingEntity entity = modelMapper.map(meeting, MeetingEntity.class);

        if (meeting.getParticipants() != null) {
            Set<EmbeddableParticipant> embeddableParticipants = meeting.getParticipants().stream()
                    .map(participant -> {
                        EmbeddableParticipant embeddable = new EmbeddableParticipant();
                        embeddable.setUserId(participant.getUser().getId());
                        embeddable.setStatus(participant.getStatus());
                        embeddable.setResponseToken(participant.getResponseToken());
                        return embeddable;
                    })
                    .collect(Collectors.toSet());
            entity.setParticipants(embeddableParticipants);
        }
        return entity;
    }

    @Override
    public Optional<Meeting> findMeetingByParticipantToken(String token) {
        return jpaRepository.findMeetingByParticipantToken(token)
                .map(this::toDomain);
    }

    @Override
    public Optional<Meeting> findCheckInEligibleMeeting(Long organizerId, Long roomId, LocalDateTime now) {
        LocalDateTime timeStartWindow = now.minusMinutes(15);
        LocalDateTime timeEndWindow = now.plusMinutes(30);

        return jpaRepository.findCheckInEligibleMeeting(organizerId, roomId, timeStartWindow, timeEndWindow)
                .map(this::toDomain);
    }

    @Override
    public List<Meeting> findUncheckedInMeetings(LocalDateTime cutoffTime) {
        return jpaRepository.findUncheckedInMeetings(cutoffTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Meeting> findMeetingsForUsersInDateRange(Set<Long> userIds, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findMeetingsForUsersInDateRange(userIds, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Meeting> findCanceledMeetingsInDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findCanceledMeetingsInDateRange(from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @PostConstruct
    public void configureMapper() {
        TypeMap<MeetingEntity, Meeting> entityToDomainMap = modelMapper.typeMap(MeetingEntity.class, Meeting.class);
        // QUAN TRỌNG: Skip để map thủ công trong toDomain()
        entityToDomainMap.addMappings(mapper -> mapper.skip(Meeting::setParticipants)); 

        TypeMap<Meeting, MeetingEntity> domainToEntityMap = modelMapper.typeMap(Meeting.class, MeetingEntity.class);
        domainToEntityMap.addMappings(mapper -> mapper.skip(MeetingEntity::setParticipants));

        modelMapper.createTypeMap(DeviceEntity.class, Device.class);
        modelMapper.createTypeMap(Device.class, DeviceEntity.class);
    }

    @Override
    public List<Meeting> findAllBySeriesId(String seriesId) {
        return jpaRepository.findAllBySeriesId(seriesId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Meeting> findMeetingsWithGuestsInDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findMeetingsWithGuestsInDateRange(from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrganizerId(Long organizerId) {
        return jpaRepository.existsByOrganizerId(organizerId);
    }

    @Override
    public Page<Meeting> findAllByUserId(Long userId, Pageable pageable) {
        Page<MeetingEntity> page = jpaRepository.findMyMeetings(userId, pageable);
        return page.map(this::toDomain);
    }

    @Override
    public List<Meeting> findConflictingMeetingsForUsers(Set<Long> userIds, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore) {
        List<MeetingEntity> entities = jpaRepository.findConflictingMeetingsForUsers(userIds, startTime, endTime, meetingIdToIgnore);
        return entities.stream()
                .map(this::toDomain) // Dùng toDomain thay vì map trực tiếp để đảm bảo nhất quán
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> findBookedDevicesInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return jpaRepository.findBookedDeviceIdsInTimeRange(startTime, endTime);
    }

    @Override
    public Page<Meeting> findAllMeetings(Pageable pageable) {
        Page<MeetingEntity> page = jpaRepository.findAllByOrderByStartTimeDesc(pageable);
        return page.map(this::toDomain); // Dùng toDomain
    }

    @Override
    public boolean isDeviceBusy(Set<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore) {
        if (deviceIds == null || deviceIds.isEmpty()) return false;
        return jpaRepository.existsConflictingDevice(deviceIds, startTime, endTime, meetingIdToIgnore);
    }

    @Override
    public List<Meeting> findFutureMeetingsByOrganizerId(Long organizerId, LocalDateTime now) {
        return jpaRepository.findFutureMeetingsByOrganizerId(organizerId, now).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> findMeetingsByRoomAndTimeRange(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        return jpaRepository.findMeetingsByRoomAndTimeRange(roomId, startTime, endTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> findMeetingsByDeviceAndTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return jpaRepository.findMeetingsByDeviceAndTimeRange(deviceId, startTime, endTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // --- ĐÃ SỬA 2 HÀM MỚI DƯỚI ĐÂY ---

    @Override
    public List<Meeting> findByStartTimeBetween(LocalDateTime start, LocalDateTime end) {
        // Sửa lỗi tên biến: springDataMeetingRepository -> jpaRepository
        List<MeetingEntity> entities = jpaRepository.findAllByStartTimeBetween(start, end);
        
        // Sửa lỗi logic: Dùng this::toDomain thay vì modelMapper.map để lấy được participants
        return entities.stream()
                .map(this::toDomain) 
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Meeting> findCurrentMeetingAtRoom(Long roomId, LocalDateTime checkTime) {
        // Sửa lỗi tên biến: springDataMeetingRepository -> jpaRepository
        Optional<MeetingEntity> entityOpt = jpaRepository.findActiveMeetingInRoom(roomId, checkTime);
        
        // Sửa lỗi logic: Dùng this::toDomain
        return entityOpt.map(this::toDomain);
    }
    @Override
    public Optional<Meeting> findByCheckinCode(String checkinCode) {
        return jpaRepository.findByCheckinCode(checkinCode)
                .map(entity -> modelMapper.map(entity, Meeting.class));
    }
}