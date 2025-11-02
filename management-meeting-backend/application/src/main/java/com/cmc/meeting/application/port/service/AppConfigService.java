package com.cmc.meeting.application.port.service;

public interface AppConfigService {
    String getValue(String key, String defaultValue);
    int getIntValue(String key, int defaultValue);
}