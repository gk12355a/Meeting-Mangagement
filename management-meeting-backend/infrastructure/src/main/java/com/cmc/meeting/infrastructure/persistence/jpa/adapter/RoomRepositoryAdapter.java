package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.RoomEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataRoomRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RoomRepositoryAdapter implements RoomRepository {

    private final SpringDataRoomRepository jpaRepository;
    private final ModelMapper modelMapper;

    public RoomRepositoryAdapter(SpringDataRoomRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<Room> findAll() {
        return jpaRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, Room.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Room> findById(Long id) {
        return jpaRepository.findById(id)
                .map(entity -> modelMapper.map(entity, Room.class));
    }
    // BỔ SUNG:
    @Override
    public Room save(Room room) {
        RoomEntity entity = modelMapper.map(room, RoomEntity.class);
        RoomEntity savedEntity = jpaRepository.save(entity);
        return modelMapper.map(savedEntity, Room.class);
    }

    // BỔ SUNG:
    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}