package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import com.cmc.meeting.domain.model.DeviceStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "devices")
public class DeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomEntity room;

    @ElementCollection
    @CollectionTable(name = "device_images", joinColumns = @JoinColumn(name = "device_id"))
    @Column(name = "image_url")
    private java.util.List<String> images;
}