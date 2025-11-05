package com.cmc.meeting.application.port.service;

import java.util.List;

import com.cmc.meeting.application.dto.admin.AppConfigUpdateRequest;
import com.cmc.meeting.domain.model.AppConfig;

public interface AppConfigService {
    String getValue(String key, String defaultValue);
    int getIntValue(String key, int defaultValue);
    List<AppConfig> getAllConfigs();
    AppConfig updateConfig(String key, AppConfigUpdateRequest request);
}