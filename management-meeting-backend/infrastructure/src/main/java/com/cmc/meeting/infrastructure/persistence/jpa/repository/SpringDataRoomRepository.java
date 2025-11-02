package com.cmc.meeting.infrastructure.persistence.jpa.repository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRoomRepository extends JpaRepository<RoomEntity, Long> {
}