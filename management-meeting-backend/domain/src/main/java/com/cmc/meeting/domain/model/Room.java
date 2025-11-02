package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private Long id;
    private String name;
    private int capacity; // Sức chứa (US-10)
    private String location;
    private List<String> fixedDevices; // Các thiết bị cố định (BS-14.2)
    private Set<Role> requiredRoles = new HashSet<>();
}