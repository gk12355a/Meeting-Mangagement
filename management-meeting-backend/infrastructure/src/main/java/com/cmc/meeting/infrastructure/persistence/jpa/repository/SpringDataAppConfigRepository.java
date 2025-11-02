package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.AppConfigEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAppConfigRepository extends JpaRepository<AppConfigEntity, String> {
    // Spring Data JPA tự hiểu findByConfigKey
    Optional<AppConfigEntity> findByConfigKey(String configKey);
}