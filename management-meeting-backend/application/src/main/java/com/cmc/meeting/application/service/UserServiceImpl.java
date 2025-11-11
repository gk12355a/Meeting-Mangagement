package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.response.UserDTO;
import com.cmc.meeting.application.port.service.UserService;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional // Đa số là readOnly
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String query) {
        // Gọi hàm mới từ Repository Port
        List<User> users = userRepository.searchByNameOrUsername(query);

        // Ánh xạ sang DTO an toàn (UserDTO chỉ có id, fullName)
        return users.stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .collect(Collectors.toList());
    }
}