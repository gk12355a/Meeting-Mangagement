package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.auth.LoginRequest;
import com.cmc.meeting.application.dto.auth.RegisterRequest;
import com.cmc.meeting.application.dto.auth.ResetPasswordRequest;
import com.cmc.meeting.application.dto.auth.AuthResponse;
import com.cmc.meeting.application.dto.auth.ForgotPasswordRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    String register(RegisterRequest registerRequest);
    String forgotPassword(ForgotPasswordRequest request);
    String resetPassword(ResetPasswordRequest request);
}