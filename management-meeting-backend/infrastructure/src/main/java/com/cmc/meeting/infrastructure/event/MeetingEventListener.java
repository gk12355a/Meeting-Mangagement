package com.cmc.meeting.infrastructure.event;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.domain.event.MeetingCreatedEvent;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant;
import com.cmc.meeting.domain.model.ParticipantStatus;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.infrastructure.notification.ThymeleafEmailService;

import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener; 
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// BỔ SUNG IMPORT NÀY:
import org.springframework.transaction.annotation.Propagation;

@Component
public class MeetingEventListener {

    private static final Logger log = LoggerFactory.getLogger(MeetingEventListener.class);
    
    private final EmailNotificationPort emailSender;
    private final MeetingRepository meetingRepository;
    private final ThymeleafEmailService thymeleafService;

    public MeetingEventListener(EmailNotificationPort emailSender, 
                                MeetingRepository meetingRepository, ThymeleafEmailService thymeleafService) {
        this.emailSender = emailSender;
        this.meetingRepository = meetingRepository;
        this.thymeleafService = thymeleafService;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMeetingCreation(MeetingCreatedEvent event) {
        log.info("EVENT RECEIVED [Async, After Commit]: Xử lý sự kiện tạo cuộc họp (ID: {}).", event.getMeetingId());
        
        try {
            // 1. Lấy chi tiết cuộc họp
            Meeting meeting = meetingRepository.findById(event.getMeetingId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy meeting cho event"));

            String subject = "Thư mời họp: " + meeting.getTitle();

            // 2. TẠO CÁC BIẾN (VARIABLES) CHUNG
            Map<String, Object> variables = new HashMap<>();
            variables.put("title", meeting.getTitle());
            variables.put("startTime", meeting.getStartTime().toString()); // (Cần format đẹp hơn sau)
            variables.put("endTime", meeting.getEndTime().toString());
            variables.put("roomName", meeting.getRoom().getName());
            variables.put("organizerName", meeting.getOrganizer().getFullName());


            // 3. GỬI CHO NHÂN VIÊN (Internal)
            // (Dùng template có nút bấm)
            String internalTemplate = "email-template.html";
            String baseUrl = "http://localhost:8080/api/v1/meetings/respond-by-link";

            for (MeetingParticipant participant : meeting.getParticipants()) {
                if (participant.getStatus() == ParticipantStatus.PENDING && participant.getResponseToken() != null) {
                    
                    log.info("-> Đang chuẩn bị HTML email (Internal) cho: {}", participant.getUser().getUsername());

                    // Tạo link phản hồi duy nhất
                    String token = participant.getResponseToken();
                    variables.put("acceptUrl", String.format("%s?token=%s&status=ACCEPTED", baseUrl, token));
                    variables.put("declineUrl", String.format("%s?token=%s&status=DECLINED", baseUrl, token));

                    String htmlBody = thymeleafService.processTemplate(internalTemplate, variables);

                    emailSender.sendHtmlEmail(
                        participant.getUser().getUsername(), 
                        subject,
                        htmlBody
                    );
                }
            }
            
            // 4. GỬI CHO KHÁCH (External) - (BS-30)
            // (Dùng template mới, không có nút bấm)
            String guestTemplate = "guest-invite-template.html";
            // (Biến 'variables' chung không cần link 'acceptUrl', 
            // Thymeleaf sẽ bỏ qua nếu template không dùng)
            
            String guestHtmlBody = thymeleafService.processTemplate(guestTemplate, variables);

            for (String guestEmail : meeting.getGuestEmails()) {
                log.info("-> Đang gửi HTML email (Guest) cho: {}", guestEmail);
                emailSender.sendHtmlEmail(
                    guestEmail,
                    subject,
                    guestHtmlBody
                );
            }

        } catch (Exception e) {
            log.error("Lỗi xử lý sự kiện tạo cuộc họp (HTML): ", e);
        }
    }
}