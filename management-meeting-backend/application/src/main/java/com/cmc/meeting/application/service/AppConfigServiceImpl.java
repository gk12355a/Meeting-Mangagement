package com.cmc.meeting.application.service;

import com.cmc.meeting.application.port.service.AppConfigService;
import com.cmc.meeting.domain.model.AppConfig;
import com.cmc.meeting.domain.port.repository.AppConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
}