package com.cmc.meeting.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;
import java.util.HashSet;

@Data
@Entity
@Table(name = "contact_groups")
public class ContactGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Quan hệ: 1 User (Owner) có Nhiều nhóm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    // Quan hệ: Nhiều Nhóm có Nhiều User (Members)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "contact_group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> members = new HashSet<>();
}