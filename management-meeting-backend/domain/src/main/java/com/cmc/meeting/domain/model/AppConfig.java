package com.cmc.meeting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    private String configKey; // Ví dụ: "auto.cancel.grace.minutes"
    private String configValue; // Ví dụ: "15"
    private String description;
}