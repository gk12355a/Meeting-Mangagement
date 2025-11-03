package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.PasswordResetToken;
import com.cmc.meeting.domain.port.repository.PasswordResetTokenRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.PasswordResetTokenEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataPasswordResetTokenRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository jpaRepository;
    private final ModelMapper modelMapper;

    public PasswordResetTokenRepositoryAdapter(SpringDataPasswordResetTokenRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken resetToken) {
        PasswordResetTokenEntity entity = modelMapper.map(resetToken, PasswordResetTokenEntity.class);
        PasswordResetTokenEntity saved = jpaRepository.save(entity);
        return modelMapper.map(saved, PasswordResetToken.class);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(entity -> modelMapper.map(entity, PasswordResetToken.class));
    }

    @Override
    public void delete(PasswordResetToken resetToken) {
        PasswordResetTokenEntity entity = modelMapper.map(resetToken, PasswordResetTokenEntity.class);
        jpaRepository.delete(entity);
    }
}