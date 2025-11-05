package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.admin.AppConfigUpdateRequest;
import com.cmc.meeting.application.port.service.AppConfigService;
import com.cmc.meeting.domain.model.AppConfig;
import com.cmc.meeting.domain.port.repository.AppConfigRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // Bổ sung
@Service
public class AppConfigServiceImpl implements AppConfigService {

    private final AppConfigRepository configRepository;

    public AppConfigServiceImpl(AppConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    @Cacheable(value = "appConfig", key = "#key") // Cache kết quả
    public String getValue(String key, String defaultValue) {
        return configRepository.findByKey(key)
                .map(AppConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    // BỔ SUNG: (US-33)
    @Override
    @Transactional(readOnly = true) // Cần Transactional (dù là readOnly)
    public List<AppConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    // BỔ SUNG: (US-33, US-32)
    @Override
    @Transactional
    // Xóa cache của key này mỗi khi cập nhật
    @CacheEvict(value = "appConfig", key = "#key") 
    public AppConfig updateConfig(String key, AppConfigUpdateRequest request) {
        AppConfig config = configRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy config key: " + key));

        config.setConfigValue(request.getConfigValue());

        return configRepository.save(config);
    }
}