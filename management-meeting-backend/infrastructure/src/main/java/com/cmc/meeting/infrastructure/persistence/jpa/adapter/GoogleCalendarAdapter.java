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
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
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

    // --- 1. TẠO MỚI (PUSH) ---
    public String pushMeetingToGoogle(Long userId, Meeting meeting) {
        try {
            Calendar service = getCalendarService(userId);
            if (service == null) return null; // User chưa link Google

            // Tạo Event Object
            Event event = mapMeetingToGoogleEvent(new Event(), meeting);

            // Gửi request INSERT
            Event executedEvent = service.events().insert("primary", event).execute();

            String googleEventId = executedEvent.getId();
            log.info("-> Đã tạo sự kiện Google Calendar mới. ID: {}", googleEventId);

            return googleEventId;

        } catch (IOException | GeneralSecurityException e) {
            log.error("Lỗi tạo sự kiện Google Calendar: ", e);
            return null;
        }
    }

    // --- 2. CẬP NHẬT (UPDATE) ---
    public void updateMeetingOnGoogle(Long userId, String googleEventId, Meeting meeting) {
        try {
            if (googleEventId == null || googleEventId.isEmpty()) return;

            Calendar service = getCalendarService(userId);
            if (service == null) return;

            // Lấy Event cũ về để giữ các thông tin metadata khác
            Event event;
            try {
                event = service.events().get("primary", googleEventId).execute();
            } catch (IOException e) {
                log.warn("Không tìm thấy Google Event ID {} để update (có thể đã bị xóa trên Calendar).", googleEventId);
                return;
            }

            // Map thông tin mới đè lên Event cũ
            event = mapMeetingToGoogleEvent(event, meeting);

            // QUAN TRỌNG: Dùng lệnh UPDATE thay vì INSERT
            service.events().update("primary", googleEventId, event).execute();

            log.info("-> Đã cập nhật Google Calendar thành công. ID: {}", googleEventId);

        } catch (Exception e) {
            log.error("Lỗi cập nhật Google Calendar: ", e);
        }
    }

    // --- 3. XÓA (DELETE) ---
    public void deleteMeetingFromGoogle(Long userId, String googleEventId) {
        try {
            if (googleEventId == null || googleEventId.isEmpty()) return;

            Calendar service = getCalendarService(userId);
            if (service == null) return;

            // Gửi lệnh DELETE
            service.events().delete("primary", googleEventId).execute();

            log.info("-> Đã xóa sự kiện trên Google Calendar: {}", googleEventId);

        } catch (Exception e) {
            // 404 (Not Found) hoặc 410 (Gone) nghĩa là đã xóa rồi -> Bỏ qua không báo lỗi
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("410"))) {
                log.warn("Google Event ID {} đã không còn tồn tại.", googleEventId);
            } else {
                log.error("Lỗi xóa Google Calendar: ", e);
            }
        }
    }

    // --- PRIVATE HELPER METHODS (TÁI SỬ DỤNG CODE) ---

    // Hàm tạo Service chung để tránh lặp code xác thực
    private Calendar getCalendarService(Long userId) throws GeneralSecurityException, IOException {
        UserEntity user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            log.warn("User ID {} không tồn tại.", userId);
            return null;
        }
        
        // Log debug trạng thái link
        // log.debug("Check Google Link User {}: Linked={}, TokenPresent={}", 
        //         user.getUsername(), user.isGoogleLinked(), user.getGoogleRefreshToken() != null);

        if (!user.isGoogleLinked() || user.getGoogleRefreshToken() == null) {
            return null;
        }

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(user.getGoogleRefreshToken())
                .build();

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CMC Meeting App")
                .build();
    }

    // Hàm Map dữ liệu chung
    private Event mapMeetingToGoogleEvent(Event event, Meeting meeting) {
        event.setSummary(meeting.getTitle())
             .setDescription(meeting.getDescription())
             .setLocation(meeting.getRoom().getName());

        // Convert Start Time
        DateTime start = convertToGoogleDateTime(meeting.getStartTime());
        event.setStart(new EventDateTime().setDateTime(start));

        // Convert End Time
        DateTime end = convertToGoogleDateTime(meeting.getEndTime());
        event.setEnd(new EventDateTime().setDateTime(end));

        return event;
    }

    private DateTime convertToGoogleDateTime(LocalDateTime localDateTime) {
        return new DateTime(localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
    }
}