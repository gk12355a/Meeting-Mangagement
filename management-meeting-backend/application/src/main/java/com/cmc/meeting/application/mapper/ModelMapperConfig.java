package com.cmc.meeting.application.mapper;

import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.domain.model.MeetingParticipant;
import com.cmc.meeting.domain.model.User;
// --- BỎ TẤT CẢ IMPORT 'infrastructure' ---

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    // BỎ: UserRepository injection

    @Bean
    public ModelMapper modelMapper() { // BỎ: Tham số UserRepository
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        
        // --- BỎ QUY TẮC 1, 2, 4, 5 (vì chúng liên quan đến 'infrastructure') ---

        // --- GIỮ LẠI QUY TẮC 3 (Domain -> DTO) ---
        // Đây là nhiệm vụ của 'application'
        modelMapper.createTypeMap(MeetingParticipant.class, MeetingDTO.UserDTO.class)
            .setConverter(context -> {
                if (context.getSource() == null || context.getSource().getUser() == null) {
                    return null;
                }
                User user = context.getSource().getUser();
                MeetingDTO.UserDTO userDTO = new MeetingDTO.UserDTO();
                userDTO.setId(user.getId());
                userDTO.setFullName(user.getFullName());
                return userDTO;
            });

        return modelMapper;
    }
}