package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.admin.AppConfigUpdateRequest;
import com.cmc.meeting.application.port.service.AppConfigService;
import com.cmc.meeting.domain.model.AppConfig; 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configs")
@Tag(name = "Admin: System Configuration API", description = "API cho Admin quản lý cấu hình (BS-33)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    private final AppConfigService appConfigService;

    public AdminConfigController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả tham số cấu hình hệ thống")
    public ResponseEntity<List<AppConfig>> getAllConfigs() {
        return ResponseEntity.ok(appConfigService.getAllConfigs());
    }

    @PutMapping("/{key}")
    @Operation(summary = "Cập nhật một tham số (vd: template email, thời gian)")
    public ResponseEntity<AppConfig> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody AppConfigUpdateRequest request) {

        AppConfig updatedConfig = appConfigService.updateConfig(key, request);
        return ResponseEntity.ok(updatedConfig);
    }
}