package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.UserEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataUserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository // Đánh dấu đây là 1 Bean của Spring
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepository;
    private final ModelMapper modelMapper;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository, ModelMapper modelMapper) {
        this.jpaRepository = jpaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        // Lấy UserEntity từ DB
        // Map nó sang User (domain model)
        return jpaRepository.findById(id)
                .map(entity -> modelMapper.map(entity, User.class));
    }
    @Override
    public Optional<User> findByUsername(String username) {
        // Lấy UserEntity từ DB
        // Map nó sang User (domain model)
        return jpaRepository.findByUsername(username)
                .map(entity -> modelMapper.map(entity, User.class));
    }
    @Override
    public User save(User user) {
        // 1. Map từ Domain Model (User) -> JPA Entity (UserEntity)
        UserEntity userEntity = modelMapper.map(user, UserEntity.class);
        
        // 2. Lưu Entity bằng JpaRepository
        UserEntity savedEntity = jpaRepository.save(userEntity);
        
        // 3. Map ngược từ Entity đã lưu -> Domain Model để trả về
        return modelMapper.map(savedEntity, User.class);
    }
    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, User.class))
                .collect(Collectors.toList());
    }
}