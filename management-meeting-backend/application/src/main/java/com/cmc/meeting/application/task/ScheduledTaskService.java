package com.cmc.meeting.application.task;

// [ĐÃ SỬA] Import đúng package từ file bạn cung cấp
import com.cmc.meeting.application.port.service.AppConfigService;
import com.cmc.meeting.application.port.service.NotificationService;

import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant; // Cần import để duyệt danh sách người tham gia
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final MeetingRepository meetingRepository;
    private final AppConfigService appConfigService;
    private final NotificationService notificationService;

    private static final int DEFAULT_GRACE_PERIOD_MINUTES = 15;

    public ScheduledTaskService(MeetingRepository meetingRepository,
                                AppConfigService appConfigService,
                                NotificationService notificationService) {
        this.meetingRepository = meetingRepository;
        this.appConfigService = appConfigService;
        this.notificationService = notificationService;
    }

    // --- TÁC VỤ 1: TỰ ĐỘNG HỦY CUỘC HỌP MA (Chạy mỗi 5 phút) ---
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoReleaseGhostMeetings() {
        log.info("SCHEDULER: Đang chạy tác vụ tự động giải phóng phòng...");

        int gracePeriodMinutes = appConfigService.getIntValue(
            "auto.cancel.grace.minutes", DEFAULT_GRACE_PERIOD_MINUTES
        );
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(gracePeriodMinutes);

        List<Meeting> ghostMeetings = meetingRepository.findUncheckedInMeetings(cutoffTime);

        if (ghostMeetings.isEmpty()) {
            return;
        }

        log.warn("SCHEDULER: Tìm thấy {} cuộc họp ma. Đang tiến hành hủy...", ghostMeetings.size());

        final String reason = String.format("Tự động hủy do không check-in sau %d phút.", gracePeriodMinutes);

        for (Meeting meeting : ghostMeetings) {
            try {
                meeting.autoCancelGhostMeeting(reason);
                meetingRepository.save(meeting);
                
                // [ĐÃ SỬA] Dùng hàm createNotification có sẵn trong interface của bạn
                // Gửi cho người tổ chức (User organizer)
                notificationService.createNotification(
                    meeting.getOrganizer(), 
                    "Lịch họp '" + meeting.getTitle() + "' đã bị hủy tự động do quá hạn check-in.",
                    meeting
                );
                
                log.info("-> Đã hủy Meeting ID: {}", meeting.getId());
            } catch (Exception e) {
                log.error("Lỗi khi hủy Meeting ID: " + meeting.getId(), e);
            }
        }
    }

    // --- TÁC VỤ 2: GỬI THÔNG BÁO NHẮC NHỞ (Chạy mỗi 1 phút) ---
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Nhắc trước 30 phút
        checkAndSendReminder(now.plusMinutes(30), "30 phút");

        // 2. Nhắc trước 15 phút
        checkAndSendReminder(now.plusMinutes(15), "15 phút");
    }

    private void checkAndSendReminder(LocalDateTime targetTime, String timeLabel) {
        LocalDateTime startRange = targetTime.minusSeconds(30);
        LocalDateTime endRange = targetTime.plusSeconds(29);

        List<Meeting> meetings = meetingRepository.findByStartTimeBetween(startRange, endRange);

        for (Meeting meeting : meetings) {
            String message = String.format("Nhắc nhở: Cuộc họp '%s' sẽ bắt đầu sau %s tại phòng %s.",
                    meeting.getTitle(), timeLabel, meeting.getRoom().getName());

            try {
                // [ĐÃ SỬA] Vì interface NotificationService không có hàm gửi cho tất cả, 
                // ta sẽ lặp qua từng người tham gia để gửi.
                if (meeting.getParticipants() != null) {
                    for (MeetingParticipant participant : meeting.getParticipants()) {
                        // Chỉ gửi cho User (thông qua hàm createNotification)
                        if (participant.getUser() != null) {
                            notificationService.createNotification(
                                participant.getUser(), 
                                message, 
                                meeting
                            );
                        }
                    }
                }
                log.info("Đã gửi reminder {} cho meeting ID: {}", timeLabel, meeting.getId());
            } catch (Exception e) {
                log.error("Lỗi gửi reminder cho meeting " + meeting.getId(), e);
            }
        }
    }
}