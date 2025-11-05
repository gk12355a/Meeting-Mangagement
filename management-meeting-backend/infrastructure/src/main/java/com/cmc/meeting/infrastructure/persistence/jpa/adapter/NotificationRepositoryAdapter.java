package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Notification;
import com.cmc.meeting.domain.port.repository.NotificationRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.NotificationEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataNotificationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository jpaRepository;
    private final ModelMapper modelMapper;

    public NotificationRepositoryAdapter(SpringDataNotificationRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = modelMapper.map(notification, NotificationEntity.class);
        NotificationEntity saved = jpaRepository.save(entity);
        return modelMapper.map(saved, Notification.class);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id).map(e -> modelMapper.map(e, Notification.class));
    }

    @Override
    public Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        Page<NotificationEntity> page = jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Notification> list = page.getContent().stream()
                .map(e -> modelMapper.map(e, Notification.class))
                .collect(Collectors.toList());
        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    @Override
    public long countByUserIdAndIsRead(Long userId, boolean isRead) {
        return jpaRepository.countByUserIdAndIsRead(userId, isRead);
    }

    @Override
    public void markAllAsReadByUserId(Long userId) {
        jpaRepository.markAllAsReadByUserId(userId);
    }
}