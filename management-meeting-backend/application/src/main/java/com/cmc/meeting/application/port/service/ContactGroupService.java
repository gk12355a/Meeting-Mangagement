package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.group.ContactGroupDTO;
import com.cmc.meeting.application.dto.group.ContactGroupRequest;
import java.util.List;

public interface ContactGroupService {
    List<ContactGroupDTO> getMyContactGroups(Long currentUserId);
    ContactGroupDTO createContactGroup(ContactGroupRequest request, Long currentUserId);
    ContactGroupDTO updateContactGroup(Long groupId, ContactGroupRequest request, Long currentUserId);
    void deleteContactGroup(Long groupId, Long currentUserId);
}