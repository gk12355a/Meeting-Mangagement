package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.AppConfig;
import com.cmc.meeting.domain.port.repository.AppConfigRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataAppConfigRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AppConfigRepositoryAdapter implements AppConfigRepository {

    private final SpringDataAppConfigRepository jpaRepository;
    private final ModelMapper modelMapper;

    public AppConfigRepositoryAdapter(SpringDataAppConfigRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Optional<AppConfig> findByKey(String key) {
        return jpaRepository.findByConfigKey(key)
                .map(entity -> modelMapper.map(entity, AppConfig.class));
    }
}