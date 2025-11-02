package com.cmc.meeting.infrastructure.event;

import com.cmc.meeting.domain.event.MeetingCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component // Đánh dấu đây là 1 Bean
public class MeetingEventListener {

    private static final Logger log = LoggerFactory.getLogger(MeetingEventListener.class);

    // (Chúng ta sẽ @Inject EmailService, GoogleCalendarService... ở đây)

    @Async // QUAN TRỌNG: Chạy tác vụ này ở luồng riêng (Yêu cầu 4.1)
    @EventListener(MeetingCreatedEvent.class)
    public void handleMeetingCreation(MeetingCreatedEvent event) {

        // Tạm dừng 3 giây để giả lập việc gửi mail/gọi API chậm
        try {
            Thread.sleep(3000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("EVENT RECEIVED [Async]: Xử lý sự kiện tạo cuộc họp.");
        log.info("-> Bắt đầu gửi email thông báo cho Meeting ID: {}", event.getMeetingId());
        log.info("-> Bắt đầu đồng bộ lên Google Calendar cho Meeting ID: {}", event.getMeetingId());

        // (Code gửi mail / đồng bộ calendar thật sẽ nằm ở đây)
    }
}