package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import com.cmc.meeting.application.dto.group.ContactGroupDTO;
import com.cmc.meeting.application.dto.group.ContactGroupRequest;
import com.cmc.meeting.application.port.service.ContactGroupService;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import com.cmc.meeting.domain.model.ContactGroup;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.ContactGroupRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContactGroupServiceImpl implements ContactGroupService {

    private final ContactGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public ContactGroupServiceImpl(ContactGroupRepository groupRepository, 
                                 UserRepository userRepository, 
                                 ModelMapper modelMapper) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactGroupDTO> getMyContactGroups(Long currentUserId) {
        return groupRepository.findAllByOwnerId(currentUserId).stream()
                .map(this::mapToDTO) // Dùng helper
                .collect(Collectors.toList());
    }

    @Override
    public ContactGroupDTO createContactGroup(ContactGroupRequest request, Long currentUserId) {
        User owner = findUserById(currentUserId);
        Set<User> members = findUsersByIds(request.getMemberIds());

        ContactGroup newGroup = new ContactGroup(request.getName(), owner, members);
        ContactGroup savedGroup = groupRepository.save(newGroup);

        return mapToDTO(savedGroup);
    }

    @Override
    public ContactGroupDTO updateContactGroup(Long groupId, ContactGroupRequest request, Long currentUserId) {
        ContactGroup group = findGroupById(groupId);

        // KIỂM TRA QUYỀN SỞ HỮU
        if (!group.getOwner().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Bạn không có quyền sửa nhóm này.");
        }

        Set<User> members = findUsersByIds(request.getMemberIds());

        group.setName(request.getName());
        group.setMembers(members);

        ContactGroup updatedGroup = groupRepository.save(group);
        return mapToDTO(updatedGroup);
    }

    @Override
    public void deleteContactGroup(Long groupId, Long currentUserId) {
        ContactGroup group = findGroupById(groupId);

        // KIỂM TRA QUYỀN SỞ HỮU
        if (!group.getOwner().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Bạn không có quyền xóa nhóm này.");
        }

        groupRepository.delete(group);
    }

    // --- Helpers ---
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy User ID: " + id));
    }

    private ContactGroup findGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Nhóm ID: " + id));
    }

    private Set<User> findUsersByIds(Set<Long> ids) {
        if (ids == null) return Set.of();
        return ids.stream()
                .map(this::findUserById)
                .collect(Collectors.toSet());
    }

    // Helper map sang DTO
    private ContactGroupDTO mapToDTO(ContactGroup group) {
        ContactGroupDTO dto = new ContactGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());

        // Map Set<User> sang Set<AdminUserDTO>
        Set<AdminUserDTO> memberDTOs = group.getMembers().stream()
            .map(member -> modelMapper.map(member, AdminUserDTO.class))
            .collect(Collectors.toSet());

        dto.setMembers(memberDTOs);
        return dto;
    }
}