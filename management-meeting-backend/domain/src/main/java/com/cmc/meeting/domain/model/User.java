package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username; // Thường là email
    private String fullName;
    private String password;
    // Chúng ta không để password ở đây. 
    // Domain model không cần biết về password.
}