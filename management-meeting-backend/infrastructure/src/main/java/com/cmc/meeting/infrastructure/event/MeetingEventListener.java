package com.cmc.meeting.infrastructure.event;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.domain.event.MeetingCancelledEvent;
import com.cmc.meeting.domain.event.MeetingCreatedEvent;
import com.cmc.meeting.domain.event.MeetingUpdatedEvent;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant;
import com.cmc.meeting.domain.model.ParticipantStatus;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.infrastructure.notification.ThymeleafEmailService;
import com.cmc.meeting.infrastructure.persistence.jpa.adapter.GoogleCalendarAdapter;
import org.springframework.beans.factory.annotation.Value;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class MeetingEventListener {

    private static final Logger log = LoggerFactory.getLogger(MeetingEventListener.class);
    @Value("${app.backend.base-url}")
    private String backendBaseUrl;
    private final EmailNotificationPort emailSender;
    private final MeetingRepository meetingRepository;
    private final ThymeleafEmailService thymeleafService;
    private final GoogleCalendarAdapter googleCalendarAdapter;

    public MeetingEventListener(EmailNotificationPort emailSender,
            MeetingRepository meetingRepository,
            ThymeleafEmailService thymeleafService,
            GoogleCalendarAdapter googleCalendarAdapter) {
        this.emailSender = emailSender;
        this.meetingRepository = meetingRepository;
        this.thymeleafService = thymeleafService;
        this.googleCalendarAdapter = googleCalendarAdapter;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMeetingCreation(MeetingCreatedEvent event) {
        log.info("EVENT RECEIVED [Async]: Xử lý sự kiện tạo cuộc họp (ID: {}).", event.getMeetingId());

        // BƯỚC 1: LẤY DỮ LIỆU (Chỉ 1 lần duy nhất)
        Meeting meeting;
        try {
            meeting = meetingRepository.findById(event.getMeetingId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Không tìm thấy meeting ID: " + event.getMeetingId()));
        } catch (Exception e) {
            log.error("Lỗi Fatal: Không lấy được thông tin cuộc họp. Dừng xử lý.", e);
            return;
        }

        // BƯỚC 2: GỬI EMAIL (Bọc trong try-catch riêng để không ảnh hưởng Google Sync)
        try {
            sendEmails(meeting);
        } catch (Exception e) {
            log.error("Lỗi khi gửi Email thông báo: ", e);
        }

        // BƯỚC 3: ĐỒNG BỘ GOOGLE CALENDAR (Bọc trong try-catch riêng)
        try {
            // Gọi hàm push và NHẬN VỀ ID
            String googleEventId = googleCalendarAdapter.pushMeetingToGoogle(
                meeting.getOrganizer().getId(), 
                meeting
            );

            // NẾU CÓ ID -> LƯU NGƯỢC VÀO DB
            if (googleEventId != null) {
                meeting.setGoogleEventId(googleEventId);
                meetingRepository.save(meeting); 
                log.info(">>> Đã lưu Google Event ID '{}' vào Database cho Meeting {}", googleEventId, meeting.getId());
            }

        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ Google Calendar: ", e);
        }
    }
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMeetingUpdate(MeetingUpdatedEvent event) {
        log.info("EVENT: Cập nhật cuộc họp ID {}, Google ID: {}", event.getMeetingId(), event.getGoogleEventId());
        try {
            Meeting meeting = meetingRepository.findById(event.getMeetingId())
                    .orElseThrow(() -> new EntityNotFoundException("Meeting not found"));

            // Gọi Adapter Update
            googleCalendarAdapter.updateMeetingOnGoogle(
                event.getUserId(), 
                event.getGoogleEventId(), 
                meeting
            );
        } catch (Exception e) {
            log.error("Lỗi handle update event: ", e);
        }
    }

    @Async
    @TransactionalEventListener
    public void handleMeetingCancellation(MeetingCancelledEvent event) {
        log.info("EVENT: Hủy cuộc họp ID {}, Google ID: {}", event.getMeetingId(), event.getGoogleEventId());
        
        // Gọi Adapter Delete
        if (event.getGoogleEventId() != null) {
            googleCalendarAdapter.deleteMeetingFromGoogle(
                event.getUserId(), 
                event.getGoogleEventId()
            );
        }
    }

    // Tách hàm private cho gọn code chính
    private void sendEmails(Meeting meeting) {
        String subject = "Thư mời họp: " + meeting.getTitle();
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", meeting.getTitle());
        variables.put("startTime", meeting.getStartTime().toString());
        variables.put("endTime", meeting.getEndTime().toString());
        variables.put("roomName", meeting.getRoom().getName());
        variables.put("organizerName", meeting.getOrganizer().getFullName());

        // 1. Gửi cho Nhân viên (Internal)
        String internalTemplateKey = "email.template.internal";
        // String baseUrl = "http://localhost:8080/api/v1/meetings/respond-by-link"; // Nên đưa vào app.properties
        String respondEndpoint = "/meetings/respond-by-link";
        String baseUrl = backendBaseUrl + respondEndpoint;
        if (meeting.getParticipants() != null) {
            for (MeetingParticipant participant : meeting.getParticipants()) {
                if (participant.getStatus() == ParticipantStatus.PENDING && participant.getResponseToken() != null) {
                    String token = participant.getResponseToken();
                    variables.put("acceptUrl", String.format("%s?token=%s&status=ACCEPTED", baseUrl, token));
                    variables.put("declineUrl", String.format("%s?token=%s&status=DECLINED", baseUrl, token));

                    String htmlBody = thymeleafService.processTemplate(internalTemplateKey, variables);
                    emailSender.sendHtmlEmail(participant.getUser().getUsername(), subject, htmlBody);
                }
            }
        }

        // 2. Gửi cho Khách (External)
        if (meeting.getGuestEmails() != null && !meeting.getGuestEmails().isEmpty()) {
            String guestTemplateKey = "email.template.guest";
            String guestHtmlBody = thymeleafService.processTemplate(guestTemplateKey, variables);

            for (String guestEmail : meeting.getGuestEmails()) {
                emailSender.sendHtmlEmail(guestEmail, subject, guestHtmlBody);
            }
        }
        log.info("Đã hoàn tất quy trình gửi email.");
    }
}