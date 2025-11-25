package com.cmc.meeting.infrastructure.persistence.jpa.repository;


import com.cmc.meeting.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.data.repository.query.Param; // <--- SỬA IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);


    // BỔ SUNG: Để Chatbot tìm ID người dùng từ tên (ví dụ: "mời Hùng")
    List<UserEntity> findByFullNameContainingIgnoreCase(String fullName);

    // Query search tổng hợp của bạn
    @Query("SELECT u FROM UserEntity u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<UserEntity> searchByNameOrUsername(@Param("query") String query);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r = 'ROLE_ADMIN' AND u.isActive = true")
    List<UserEntity> findAllAdmins();
}