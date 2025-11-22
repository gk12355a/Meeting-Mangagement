package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.UserEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataUserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class GoogleAuthService {

    private final SpringDataUserRepository userRepository;

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.client.secret}")
    private String clientSecret;
    @Value("${google.redirect.uri}")
    private String redirectUri;

    public GoogleAuthService(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Tạo Flow
    private GoogleAuthorizationCodeFlow getFlow() throws Exception {
        GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
        web.setClientId(clientId);
        web.setClientSecret(clientSecret);
        GoogleClientSecrets secrets = new GoogleClientSecrets().setWeb(web);

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                secrets,
                Collections.singletonList(CalendarScopes.CALENDAR))
                .setAccessType("offline") // Quan trọng: offline để lấy Refresh Token
                .setApprovalPrompt("force") 
                .build();
    }

    // 1. Lấy URL đăng nhập
    public String getAuthorizationUrl() {
        try {
            return getFlow().newAuthorizationUrl().setRedirectUri(redirectUri).build();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL Google Auth", e);
        }
    }

    // 2. Đổi Code lấy Token và Lưu DB
    @Transactional
    public void exchangeAndSaveToken(String code, Long userId) {
        try {
            GoogleTokenResponse response = getFlow().newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            String refreshToken = response.getRefreshToken();
            if (refreshToken == null) throw new RuntimeException("Không lấy được Refresh Token");

            UserEntity user = userRepository.findById(userId).orElseThrow();
            user.setGoogleRefreshToken(refreshToken);
            user.setGoogleLinked(true);
            userRepository.save(user);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác thực Google", e);
        }
    }
}