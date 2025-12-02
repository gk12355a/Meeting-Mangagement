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
    // Spring Data tự động sinh query: SELECT ... FROM ... WHERE LOWER(fullName) LIKE LOWER(%name%)
    List<UserEntity> findByFullNameContainingIgnoreCase(String fullName);

    // Query search tổng hợp (Tìm theo Tên hoặc Username)
    @Query("SELECT u FROM UserEntity u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<UserEntity> searchByNameOrUsername(@Param("query") String query);

    // --- SỬA QUERY NÀY ---
    // Giả sử RoleEntity có trường 'name' lưu tên role (ví dụ: ROLE_ADMIN)
    // Và UserEntity có trường 'isActive' (hoặc 'enabled', hãy kiểm tra lại entity của bạn)
    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN' AND u.isActive = true")
    List<UserEntity> findAllAdmins();
}