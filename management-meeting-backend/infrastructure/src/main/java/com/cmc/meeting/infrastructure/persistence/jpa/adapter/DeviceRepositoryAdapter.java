package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Device;
import com.cmc.meeting.domain.port.repository.DeviceRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.DeviceEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataDeviceRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DeviceRepositoryAdapter implements DeviceRepository {

    private final SpringDataDeviceRepository jpaRepository;
    private final ModelMapper modelMapper;

    public DeviceRepositoryAdapter(SpringDataDeviceRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<Device> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> modelMapper.map(e, Device.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Device> findById(Long id) {
        return jpaRepository.findById(id)
                .map(e -> modelMapper.map(e, Device.class));
    }

    @Override
    public Device save(Device device) {
        DeviceEntity entity = modelMapper.map(device, DeviceEntity.class);
        DeviceEntity savedEntity = jpaRepository.save(entity);
        return modelMapper.map(savedEntity, Device.class);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}