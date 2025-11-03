package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.ContactGroup;
import com.cmc.meeting.domain.port.repository.ContactGroupRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.ContactGroupEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataContactGroupRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ContactGroupRepositoryAdapter implements ContactGroupRepository {

    private final SpringDataContactGroupRepository jpaRepository;
    private final ModelMapper modelMapper;

    public ContactGroupRepositoryAdapter(SpringDataContactGroupRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public ContactGroup save(ContactGroup group) {
        ContactGroupEntity entity = modelMapper.map(group, ContactGroupEntity.class);
        ContactGroupEntity savedEntity = jpaRepository.save(entity);
        return modelMapper.map(savedEntity, ContactGroup.class);
    }

    @Override
    public Optional<ContactGroup> findById(Long id) {
        return jpaRepository.findById(id)
                .map(entity -> modelMapper.map(entity, ContactGroup.class));
    }

    @Override
    public void delete(ContactGroup group) {
        ContactGroupEntity entity = modelMapper.map(group, ContactGroupEntity.class);
        jpaRepository.delete(entity);
    }

    @Override
    public List<ContactGroup> findAllByOwnerId(Long ownerId) {
        return jpaRepository.findAllByOwnerId(ownerId).stream()
                .map(entity -> modelMapper.map(entity, ContactGroup.class))
                .collect(Collectors.toList());
    }
}