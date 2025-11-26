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
import com.google.auth.oauth2.UserCredentials; // Thay thế GoogleCredential
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

    public String pushMeetingToGoogle(Long userId, Meeting meeting) {
        try {
            // 1. Kiểm tra user có liên kết Google không
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("DEBUG: User ID {} không tồn tại.", userId);
                return null;
            }

            log.info("DEBUG: User {} - Linked: {} - Token: {}",
                    user.getUsername(),
                    user.isGoogleLinked(),
                    (user.getGoogleRefreshToken() != null ? "CÓ" : "KHÔNG"));

            if (!user.isGoogleLinked() || user.getGoogleRefreshToken() == null) {
                return null;
            }

            // 2. Tạo UserCredentials (thay thế GoogleCredential)
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(user.getGoogleRefreshToken())
                    .build();

            // 3. Khởi tạo Calendar Service
            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CMC Meeting App")
                    .build();

            // 4. Tạo Event
            Event event = new Event()
                    .setSummary(meeting.getTitle())
                    .setLocation(meeting.getRoom().getName())
                    .setDescription(meeting.getDescription());

            DateTime start = new DateTime(meeting.getStartTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(start));

            DateTime end = new DateTime(meeting.getEndTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(end));

            // 5. Gửi request → LẤY EVENT ID
            Event executedEvent = service.events().insert("primary", event).execute();

            String googleEventId = executedEvent.getId();
            log.info("-> Đã đồng bộ Google Calendar cho user: {} - Event ID: {}",
                    user.getUsername(),
                    googleEventId);

            return googleEventId;
        } catch (IOException | GeneralSecurityException e) {
            log.error("Lỗi đồng bộ Google Calendar: ", e);
            return null;
        }
    }

    public void updateMeetingOnGoogle(Long userId, String googleEventId, Meeting meeting) {
        try {
            // 1. Check điều kiện
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isGoogleLinked() || user.getGoogleRefreshToken() == null)
                return;
            if (googleEventId == null || googleEventId.isEmpty())
                return;

            // 2. Tạo Service (Auth)
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(user.getGoogleRefreshToken())
                    .build();

            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CMC Meeting App")
                    .build();

            // 3. Lấy Event cũ về (để giữ các thông tin khác không đổi)
            Event event;
            try {
                event = service.events().get("primary", googleEventId).execute();
            } catch (Exception e) {
                log.warn("Không tìm thấy sự kiện trên Google để update (ID: {}). Có thể đã bị xóa.", googleEventId);
                return;
            }

            // 4. Cập nhật thông tin mới từ App
            event.setSummary(meeting.getTitle());
            event.setDescription(meeting.getDescription());
            event.setLocation(meeting.getRoom().getName());

            // Update thời gian
            DateTime start = new DateTime(
                    meeting.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(start));

            DateTime end = new DateTime(meeting.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(end));

            // 5. Gửi lệnh Update
            service.events().update("primary", googleEventId, event).execute();

            log.info("-> Đã cập nhật Google Calendar thành công. Google ID: {}", googleEventId);

        } catch (Exception e) {
            log.error("Lỗi cập nhật Google Calendar: ", e);
        }
    }

    public void deleteMeetingFromGoogle(Long userId, String googleEventId) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isGoogleLinked() || user.getGoogleRefreshToken() == null)
                return;
            if (googleEventId == null || googleEventId.isEmpty())
                return;

            // Tạo Service (Auth) - Code lặp lại, nên tách private method nếu muốn gọn
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(user.getGoogleRefreshToken())
                    .build();

            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CMC Meeting App")
                    .build();

            // Gửi lệnh Xóa
            service.events().delete("primary", googleEventId).execute();

            log.info("-> Đã xóa sự kiện trên Google Calendar: {}", googleEventId);

        } catch (Exception e) {
            // 404 Not Found (Đã xóa rồi) -> Bỏ qua
            // 410 Gone (Đã xóa hẳn) -> Bỏ qua
            log.warn("Lỗi khi xóa lịch Google (có thể đã xóa trước đó): " + e.getMessage());
        }
    }
}