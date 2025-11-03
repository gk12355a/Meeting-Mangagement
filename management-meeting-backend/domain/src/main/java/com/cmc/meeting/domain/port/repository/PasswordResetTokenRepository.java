package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken resetToken);
    Optional<PasswordResetToken> findByToken(String token);
    void delete(PasswordResetToken resetToken);
}