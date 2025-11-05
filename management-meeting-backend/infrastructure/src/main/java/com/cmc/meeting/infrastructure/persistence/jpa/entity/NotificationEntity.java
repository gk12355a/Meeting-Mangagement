package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user; // Thông báo của ai

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id") // Có thể null
    private MeetingEntity meeting; // Link tới cuộc họp
}