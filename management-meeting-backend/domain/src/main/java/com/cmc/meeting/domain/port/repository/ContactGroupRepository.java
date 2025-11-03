package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.ContactGroup;
import java.util.List;
import java.util.Optional;

public interface ContactGroupRepository {
    ContactGroup save(ContactGroup group);
    Optional<ContactGroup> findById(Long id);
    void delete(ContactGroup group);

    // API chính: Tìm các nhóm của 1 user
    List<ContactGroup> findAllByOwnerId(Long ownerId);
}