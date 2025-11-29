package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.request.UserProfileUpdateRequest;
import com.cmc.meeting.application.dto.response.UserDTO;
import com.cmc.meeting.application.port.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateUserProfile(
            @RequestBody UserProfileUpdateRequest request,
            @AuthenticationPrincipal Object principal) { // Dùng Object
        
        String currentUsername;
        if (principal instanceof UserDetails) {
            currentUsername = ((UserDetails) principal).getUsername();
        } else if (principal instanceof Jwt) {
            currentUsername = ((Jwt) principal).getSubject();
        } else {
            throw new RuntimeException("Auth type not supported");
        }
        
        UserDTO updatedUser = userService.updateUserProfile(currentUsername, request);
        return ResponseEntity.ok(updatedUser);
    }
}