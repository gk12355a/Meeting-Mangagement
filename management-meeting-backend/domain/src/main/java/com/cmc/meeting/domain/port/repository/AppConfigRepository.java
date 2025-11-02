package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.AppConfig;
import java.util.Optional;

public interface AppConfigRepository {
    Optional<AppConfig> findByKey(String key);
    // (Chúng ta sẽ thêm 'save' và 'findAll' sau nếu cần API cho Admin)
}