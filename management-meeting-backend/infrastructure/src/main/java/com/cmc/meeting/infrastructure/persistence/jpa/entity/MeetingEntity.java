package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import com.cmc.meeting.domain.model.BookingStatus;
import com.cmc.meeting.infrastructure.persistence.jpa.embeddable.EmbeddableParticipant;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

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
    @Column(nullable = false)
    private BookingStatus status;

    // --- Quan hệ (Relationships) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private UserEntity organizer;

    @ElementCollection(fetch = FetchType.EAGER) // Lấy danh sách participant ngay
    @CollectionTable(name = "meeting_participants", joinColumns = @JoinColumn(name = "meeting_id"))
    private Set<EmbeddableParticipant> participants;
}