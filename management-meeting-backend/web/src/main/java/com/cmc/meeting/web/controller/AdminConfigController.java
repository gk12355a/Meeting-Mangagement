// package com.cmc.meeting.web.controller;

// import com.cmc.meeting.application.port.service.AppConfigService;
// // (Sắp tới chúng ta sẽ tạo AppConfigService)
// // import com.cmc.meeting.domain.model.AppConfig; 
// import io.swagger.v3.oas.annotations.tags.Tag;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// // (Đây là bộ API đơn giản, chúng ta sẽ cần
// // AppConfigService hoàn chỉnh hơn để 'getAll' và 'update')

// @RestController
// @RequestMapping("/api/v1/admin/configs")
// @Tag(name = "Admin: System Configuration API", description = "API cho Admin quản lý cấu hình (BS-33)")
// @PreAuthorize("hasRole('ADMIN')")
// public class AdminConfigController {

//     // (Chúng ta sẽ hoàn thiện API này sau khi nâng cấp AppConfigService)

//     // @GetMapping
//     // public ResponseEntity<List<AppConfig>> getAllConfigs() { ... }

//     // @PutMapping("/{key}")
//     // public ResponseEntity<AppConfig> updateConfig(...) { ... }
// }