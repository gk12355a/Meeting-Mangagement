package com.cmc.meeting.infrastructure.event;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.domain.event.MeetingCreatedEvent;
import com.cmc.meeting.domain.model.Meeting;
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
            String baseUrl = "http://localhost:8080/api/v1/meetings/respond-by-link"; // (API chúng ta sẽ tạo)

            // 2. Lặp qua từng người tham dự
            meeting.getParticipants().forEach(participant -> {
                
                // Chỉ gửi cho người PENDING (và có token)
                if (participant.getStatus() == com.cmc.meeting.domain.model.ParticipantStatus.PENDING 
                        && participant.getResponseToken() != null) {
                    
                    log.info("-> Đang chuẩn bị HTML email cho: {}", participant.getUser().getUsername());

                    // 3. TẠO CÁC BIẾN (VARIABLES) CHO TEMPLATE
                    // (Làm điều này BÊN TRONG vòng lặp)
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("title", meeting.getTitle());
                    variables.put("startTime", meeting.getStartTime().toString());
                    variables.put("endTime", meeting.getEndTime().toString());
                    variables.put("roomName", meeting.getRoom().getName());
                    variables.put("organizerName", meeting.getOrganizer().getFullName());
                    
                    // 4. TẠO LINK PHẢN HỒI DUY NHẤT
                    String token = participant.getResponseToken();
                    String acceptUrl = String.format("%s?token=%s&status=ACCEPTED", baseUrl, token);
                    String declineUrl = String.format("%s?token=%s&status=DECLINED", baseUrl, token);

                    variables.put("acceptUrl", acceptUrl);
                    variables.put("declineUrl", declineUrl);

                    // 5. "VẼ" HTML
                    String htmlBody = thymeleafService.processTemplate("email-template.html", variables);

                    // 6. Gửi email
                    emailSender.sendHtmlEmail(
                        participant.getUser().getUsername(), 
                        subject,
                        htmlBody
                    );
                }
            });

        } catch (Exception e) {
            log.error("Lỗi xử lý sự kiện tạo cuộc họp (HTML): ", e);
        }
    }
}