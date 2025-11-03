package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.ContactGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataContactGroupRepository extends JpaRepository<ContactGroupEntity, Long> {
    // Spring Data JPA tự hiểu
    List<ContactGroupEntity> findAllByOwnerId(Long ownerId);
}