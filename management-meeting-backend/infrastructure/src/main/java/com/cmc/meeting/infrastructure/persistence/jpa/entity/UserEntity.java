package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import com.cmc.meeting.domain.model.Role; // Bổ sung
import jakarta.persistence.*;
import lombok.Data;
import java.util.Set; // Bổ sung
import java.util.HashSet; // Bổ sung

@Data
@Entity
@Table(name = "users") // Đặt tên bảng là "users"
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // email

    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String password;
    @ElementCollection(fetch = FetchType.EAGER) // Tải quyền ngay lập tức
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name", nullable = false)
    @Enumerated(EnumType.STRING) // Lưu tên (vd: "ROLE_ADMIN")
    private Set<Role> roles = new HashSet<>();
    @Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 1")
    private boolean isActive = true;
  
}