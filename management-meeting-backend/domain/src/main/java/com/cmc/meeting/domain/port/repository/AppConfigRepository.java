package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.AppConfig;

import java.util.List;
import java.util.Optional;

public interface AppConfigRepository {
    Optional<AppConfig> findByKey(String key);
    List<AppConfig> findAll();
    AppConfig save(AppConfig config);
}