package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Device;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant; // Bổ sung
import com.cmc.meeting.domain.model.User; // Bổ sung
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.UserRepository; // Bổ sung
import com.cmc.meeting.infrastructure.persistence.jpa.entity.DeviceEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.embeddable.EmbeddableParticipant; // Bổ sung
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataMeetingRepository;
import jakarta.annotation.PostConstruct;

// Bỏ: @PostConstruct
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// Bỏ: TypeMap
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set; // Bổ sung
import java.util.stream.Collectors;

@Repository
public class MeetingRepositoryAdapter implements MeetingRepository {

    private final SpringDataMeetingRepository jpaRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository; // Bổ sung

    // CẬP NHẬT CONSTRUCTOR
    public MeetingRepositoryAdapter(SpringDataMeetingRepository jpaRepository,
            ModelMapper modelMapper,
            UserRepository userRepository) { // Bổ sung
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository; // Bổ sung
    }

    // BỎ: @PostConstruct

    @Override
    public Meeting save(Meeting meeting) {
        // Map thủ công Domain -> Entity
        MeetingEntity entity = toEntity(meeting);
        MeetingEntity savedEntity = jpaRepository.save(entity);
        // Map thủ công Entity -> Domain
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Meeting> findById(Long id) {
        // Map thủ công Entity -> Domain
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean isRoomBusy(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore) {
        return jpaRepository.findRoomOverlap(roomId, startTime, endTime, meetingIdToIgnore);
    }

    @Override
    public List<Meeting> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(this::toDomain) // Map thủ công
                .collect(Collectors.toList());
    }

    @Override
    public List<Meeting> findConfirmedMeetingsInDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findConfirmedMeetingsInDateRange(from, to).stream()
                .map(this::toDomain) // Map thủ công
                .collect(Collectors.toList());
    }

    // --- BỔ SUNG LẠI CÁC HÀM HELPER ---

    /**
     * Chuyển đổi Entity (DB) -> Domain (App)
     * Map thủ công trường 'participants'
     */
    private Meeting toDomain(MeetingEntity entity) {
        if (entity == null)
            return null;

        // 1. Map các trường đơn giản
        Meeting meeting = modelMapper.map(entity, Meeting.class);

        // 2. Map thủ công 'participants' (Embeddable -> Domain)
        Set<MeetingParticipant> participants = entity.getParticipants().stream()
                .map(embeddable -> {
                    // "Làm đầy" (Hydrate) User object
                    User user = userRepository.findById(embeddable.getUserId()).orElse(null);
                    if (user == null)
                        return null;
                    return new MeetingParticipant(user, embeddable.getStatus(), embeddable.getResponseToken());
                })
                .filter(p -> p != null)
                .collect(Collectors.toSet());
        meeting.setParticipants(participants);

        return meeting;
    }

    /**
     * Chuyển đổi Domain (App) -> Entity (DB)
     * Map thủ công trường 'participants'
     */
    private MeetingEntity toEntity(Meeting meeting) {
        if (meeting == null)
            return null;

        // 1. Map các trường đơn giản
        MeetingEntity entity = modelMapper.map(meeting, MeetingEntity.class);

        // 2. Map thủ công 'participants' (Domain -> Embeddable)
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

        return entity;
    }

    @Override
    public Optional<Meeting> findMeetingByParticipantToken(String token) {
        return jpaRepository.findMeetingByParticipantToken(token)
                .map(this::toDomain); // Dùng lại helper toDomain
    }

    @Override
    public Optional<Meeting> findCheckInEligibleMeeting(Long organizerId, Long roomId, LocalDateTime now) {
        // Định nghĩa cửa sổ check-in (vd: 15 phút trước, 30 phút sau)
        LocalDateTime timeStartWindow = now.minusMinutes(15);
        LocalDateTime timeEndWindow = now.plusMinutes(30);

        return jpaRepository.findCheckInEligibleMeeting(
                organizerId,
                roomId,
                timeStartWindow,
                timeEndWindow)
                .map(this::toDomain); // Dùng lại helper toDomain
    }

    @Override
    public List<Meeting> findUncheckedInMeetings(LocalDateTime cutoffTime) {
        return jpaRepository.findUncheckedInMeetings(cutoffTime).stream()
                .map(this::toDomain) // Dùng lại helper toDomain
                .collect(Collectors.toList());
    }

    // BỔ SUNG: (US-5)
    @Override
    public List<Meeting> findMeetingsForUsersInDateRange(Set<Long> userIds, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findMeetingsForUsersInDateRange(userIds, from, to).stream()
                .map(this::toDomain) // Dùng lại helper toDomain
                .collect(Collectors.toList());
    }

    // BỔ SUNG: (US-23)
    @Override
    public List<Meeting> findCanceledMeetingsInDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findCanceledMeetingsInDateRange(from, to).stream()
                .map(this::toDomain) // Dùng lại helper toDomain
                .collect(Collectors.toList());
    }

    @PostConstruct
    public void configureMapper() {
        // Cấu hình (cũ) cho Meeting <-> MeetingEntity
        TypeMap<MeetingEntity, Meeting> entityToDomainMap = modelMapper.typeMap(MeetingEntity.class, Meeting.class);
        entityToDomainMap.addMappings(mapper -> mapper.skip(Meeting::setParticipants));

        TypeMap<Meeting, MeetingEntity> domainToEntityMap = modelMapper.typeMap(Meeting.class, MeetingEntity.class);
        domainToEntityMap.addMappings(mapper -> mapper.skip(MeetingEntity::setParticipants));

        // --- BỔ SUNG: Dạy cách map Device (Vì tên trường giống hệt nhau) ---
        modelMapper.createTypeMap(DeviceEntity.class, Device.class);
        modelMapper.createTypeMap(Device.class, DeviceEntity.class);
    }

    @Override
    public List<Meeting> findAllBySeriesId(String seriesId) {
        return jpaRepository.findAllBySeriesId(seriesId).stream()
                .map(this::toDomain) // Dùng lại helper toDomain
                .collect(Collectors.toList());
    }

    // BỔ SUNG: (BS-31)
    @Override
    public List<Meeting> findMeetingsWithGuestsInDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findMeetingsWithGuestsInDateRange(from, to).stream()
                .map(this::toDomain) // Dùng lại helper toDomain
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrganizerId(Long organizerId) {
        return jpaRepository.existsByOrganizerId(organizerId);
    }

    // CẬP NHẬT: (US-6)
    @Override
    public Page<Meeting> findAllByUserId(Long userId, Pageable pageable) {
        Page<MeetingEntity> page = jpaRepository.findMyMeetings(userId, pageable);
        return page.map(this::toDomain); // Dùng helper toDomain
    }

    // CẬP NHẬT: (US-5)
    @Override
    public List<Meeting> findConflictingMeetingsForUsers(Set<Long> userIds, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore) {
        List<MeetingEntity> entities = jpaRepository.findConflictingMeetingsForUsers(userIds, startTime, endTime, meetingIdToIgnore);
        return entities.stream()
                .map(entity -> modelMapper.map(entity, Meeting.class))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> findBookedDevicesInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // Chỉ cần gọi thẳng hàm của JPA Repo
        return jpaRepository.findBookedDeviceIdsInTimeRange(startTime, endTime);
    }

    @Override
    public Page<Meeting> findAllMeetings(Pageable pageable) {
        // 1. Gọi hàm JPA mới
        Page<MeetingEntity> page = jpaRepository.findAllByOrderByStartTimeDesc(pageable);

        // 2. Map Entity Page -> Domain Model Page
        return page.map(entity -> modelMapper.map(entity, Meeting.class));
    }

    @Override
    public boolean isDeviceBusy(Set<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return false;
        }
        return jpaRepository.existsConflictingDevice(deviceIds, startTime, endTime, meetingIdToIgnore);
    }
}