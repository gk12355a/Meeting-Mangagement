package com.cmc.meeting.infrastructure.persistence.jpa.adapter;

import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.UserEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.repository.SpringDataUserRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter; // Adapter mới
import com.google.auth.oauth2.UserCredentials;    // Thay thế GoogleCredential
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;

@Component
public class GoogleCalendarAdapter {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);
    private final SpringDataUserRepository userRepository;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    public GoogleCalendarAdapter(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void pushMeetingToGoogle(Long userId, Meeting meeting) {
        try {
            // 1. Kiểm tra user có liên kết Google không
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("DEBUG: User ID {} không tồn tại.", userId);
                return;
            }
            log.info("DEBUG: User {} - Linked: {} - Token: {}", 
                     user.getUsername(), 
                     user.isGoogleLinked(), 
                     (user.getGoogleRefreshToken() != null ? "CÓ" : "KHÔNG"));
            if (user == null || !user.isGoogleLinked() || user.getGoogleRefreshToken() == null) {
                return; 
            }

            // 2.  Sử dụng UserCredentials thay vì GoogleCredential (Deprecated)
            // Thư viện này tự động xử lý việc Refresh Token khi hết hạn
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(user.getGoogleRefreshToken())
                    .build();

            // 3. Khởi tạo Calendar Service
            // Dùng HttpCredentialsAdapter để chuyển đổi từ Auth Library sang API Client
            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)) //Adapter ở đây
                    .setApplicationName("CMC Meeting App")
                    .build();

            // 4. Tạo Event (Giữ nguyên logic cũ)
            Event event = new Event()
                    .setSummary(meeting.getTitle())
                    .setLocation(meeting.getRoom().getName())
                    .setDescription(meeting.getDescription());

            DateTime start = new DateTime(meeting.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(start));

            DateTime end = new DateTime(meeting.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(end));

            // 5. Gửi request
            service.events().insert("primary", event).execute();
            log.info("-> Đã đồng bộ Google Calendar cho user: {}", user.getUsername());

        } catch (IOException | GeneralSecurityException e) {
            log.error("Lỗi đồng bộ Google Calendar: ", e);
        }
    }
}