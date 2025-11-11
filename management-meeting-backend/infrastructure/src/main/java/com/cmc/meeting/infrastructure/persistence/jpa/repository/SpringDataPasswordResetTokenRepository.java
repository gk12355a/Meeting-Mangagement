package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.PasswordResetTokenEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
    Optional<PasswordResetTokenEntity> findByUser(UserEntity user);
}