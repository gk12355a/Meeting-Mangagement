package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.UserEntity;

import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByAuthServiceId(Long authServiceId); // Spring Data JPA tự hiểu

    @Query("SELECT u FROM UserEntity u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "(LENGTH(u.fullName) >= 3 AND LOWER(:query) LIKE LOWER(CONCAT('%', u.fullName, '%'))) OR " +
            "(LENGTH(u.username) >= 3 AND LOWER(:query) LIKE LOWER(CONCAT('%', u.username, '%')))")
    List<UserEntity> searchByNameOrUsername(@Param("query") String query);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r = 'ROLE_ADMIN' AND u.isActive = true")
    List<UserEntity> findAllAdmins();
}