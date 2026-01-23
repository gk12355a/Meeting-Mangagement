package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

import com.cmc.meeting.domain.model.RoomStatus;

@Data
@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int capacity;

    // Giữ lại để reference, nhưng dữ liệu chính sẽ là buildingName + floor
    private String location;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "floor")
    private Integer floor;

    // Lưu danh sách thiết bị cố định (BS-14.2)
    @ElementCollection
    @CollectionTable(name = "room_devices", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "device_name")
    private List<String> fixedDevices;

    // BỔ SUNG: (BS-11.1)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = false;

    @ElementCollection
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image_url")
    private List<String> images;
}