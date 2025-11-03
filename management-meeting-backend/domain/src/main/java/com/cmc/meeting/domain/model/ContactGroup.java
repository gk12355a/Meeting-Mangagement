package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
public class ContactGroup {
    private Long id;
    private String name;

    // Ai là người tạo/sở hữu nhóm này
    private User owner; 

    // Ai nằm trong nhóm này
    private Set<User> members = new HashSet<>();

    public ContactGroup(String name, User owner, Set<User> members) {
        this.name = name;
        this.owner = owner;
        this.members = members;
    }
}