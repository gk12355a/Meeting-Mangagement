package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import com.cmc.meeting.domain.model.BookingStatus;
import com.cmc.meeting.infrastructure.persistence.jpa.embeddable.EmbeddableParticipant;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.cmc.meeting.domain.model.Role;

@Data
@Entity
@Table(name = "meetings")
public class MeetingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob // Dành cho text dài
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING) // Lưu tên của Enum (CONFIRMED, CANCELLED)
    @Column(name = "status", length = 50)
    private BookingStatus status;

    // --- Quan hệ (Relationships) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator; // Người tạo (người gọi API)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private UserEntity organizer;

    @ElementCollection(fetch = FetchType.EAGER) // Lấy danh sách participant ngay
    @CollectionTable(name = "meeting_participants", joinColumns = @JoinColumn(name = "meeting_id"))
    private Set<EmbeddableParticipant> participants;

    @Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 0")
    private boolean isCheckedIn = false;

    @Column
    private String cancelReason;

    @Column
    private LocalDateTime cancelledAt;

    // BỔ SUNG: (US-12)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "meeting_devices", // Tên bảng trung gian
            joinColumns = @JoinColumn(name = "meeting_id"), inverseJoinColumns = @JoinColumn(name = "device_id"))
    private Set<DeviceEntity> devices = new HashSet<>();

    @Column(nullable = true, length = 36) // UUID
    private String seriesId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_required_roles", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "role_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> requiredRoles = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "meeting_guests", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "email", nullable = false)
    private Set<String> guestEmails = new HashSet<>();

    @Column(name = "checkin_code", unique = true)
    private String checkinCode;
}