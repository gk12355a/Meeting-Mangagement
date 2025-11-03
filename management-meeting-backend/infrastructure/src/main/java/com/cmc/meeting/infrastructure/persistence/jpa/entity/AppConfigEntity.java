package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "app_configuration")
public class AppConfigEntity {

    @Id
    @Column(length = 100)
    private String configKey;

    // CẬP NHẬT:
    @Lob // Dùng cho kiểu TEXT (dài)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configValue;

    private String description;
}