package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
 
    Optional<User> findByUsername(String username);
    User save(User user);
}