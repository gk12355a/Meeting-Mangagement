package com.cmc.meeting.infrastructure.notification;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.application.port.service.AppConfigService;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.User; // Bổ sung
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationAdapter implements EmailNotificationPort {

    private final JavaMailSender javaMailSender;
    private final ThymeleafEmailService thymeleafService;
    private final AppConfigService appConfigService;

    public EmailNotificationAdapter(JavaMailSender javaMailSender,
                                    ThymeleafEmailService thymeleafService,
                                    AppConfigService appConfigService) {
        this.javaMailSender = javaMailSender;
        this.thymeleafService = thymeleafService;
        this.appConfigService = appConfigService;
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            
            // Tham số boolean (multipart) phải đứng TRƯỚC String (encoding)
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, 
                    true,     // <-- 1. multipart (boolean)
                    "UTF-8"   // <-- 2. Mã hóa (String)
            );


            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail HTML: " + e.getMessage());
        }
    }
    @Override
    public void sendPasswordResetEmail(User user, String token) {
        try {
            String subject = "Yêu cầu Đặt lại Mật khẩu";
            String frontendResetUrl = appConfigService.getValue("frontend.url", "http://localhost:5173"); // Lấy URL frontend
            String resetUrl = String.format("%s/reset-password?token=%s", frontendResetUrl, token);

            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getFullName());
            variables.put("resetUrl", resetUrl);

            // Lấy template từ CSDL (BS-32) và "vẽ" HTML
            String htmlBody = thymeleafService.processTemplate(
                "email.template.forgot-password", 
                variables
            );

            // Gửi mail (tái sử dụng logic)
            this.sendHtmlEmail(user.getUsername(), subject, htmlBody);

        } catch (Exception e) {
            System.err.println("Lỗi gửi mail reset mật khẩu: " + e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(User user, String rawPassword) {
        // 1. Định nghĩa key (phải khớp với CSDL)
        final String TEMPLATE_KEY = "email.template.welcome";
        final String subject = "Chào mừng bạn đến với Hệ thống Đặt lịch họp";

        // 2. Tạo biến (variables) cho Thymeleaf
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", user.getFullName());
        variables.put("username", user.getUsername());
        variables.put("rawPassword", rawPassword);
        variables.put("loginUrl", appConfigService.getValue("frontend.base-url", "http://localhost:5173") + "/login");

        // 3. Xử lý template
        String htmlBody = thymeleafService.processTemplate(TEMPLATE_KEY, variables);

        // 4. Gửi email
        this.sendHtmlEmail(user.getUsername(), subject, htmlBody);
    }
    // Gửi nhắc nhở (Dùng chung cho cả User nội bộ và Guest)
    @Override
    public void sendMeetingReminder(String toEmail, Meeting meeting, String timeLabel) {
        try {
            // 1. Định nghĩa key template (Cần thêm vào DB/Config)
            final String TEMPLATE_KEY = "email.template.meeting-reminder";
            final String subject = "Nhắc nhở: Cuộc họp sắp diễn ra - " + meeting.getTitle();

            // 2. Format dữ liệu
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            String baseUrl = appConfigService.getValue("frontend.base-url", "http://localhost:5173");
            
            String organizerName = (meeting.getOrganizer() != null) ? meeting.getOrganizer().getFullName() : "Ban tổ chức";

            // 3. Đóng gói biến
            Map<String, Object> variables = new HashMap<>();
            variables.put("title", meeting.getTitle());
            variables.put("timeLabel", timeLabel); // "15 phút" hoặc "30 phút"
            variables.put("roomName", meeting.getRoom().getName());
            variables.put("startTime", meeting.getStartTime().format(formatter));
            variables.put("endTime", meeting.getEndTime().format(formatter));
            variables.put("organizer", organizerName);
            variables.put("meetingUrl", baseUrl + "/meetings/" + meeting.getId());

            // 4. Xử lý template (Giống hệt các hàm trên)
            String htmlBody = thymeleafService.processTemplate(TEMPLATE_KEY, variables);

            // 5. Gửi
            this.sendHtmlEmail(toEmail, subject, htmlBody);

        } catch (Exception e) {
            System.err.println("Lỗi gửi email nhắc nhở tới " + toEmail + ": " + e.getMessage());
        }
    }
}