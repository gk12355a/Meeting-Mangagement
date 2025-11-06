package com.cmc.meeting.infrastructure.notification;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.application.port.service.AppConfigService;
import com.cmc.meeting.domain.model.User; // Bổ sung
import jakarta.mail.internet.MimeMessage;

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
            
            // === SỬA LỖI Ở ĐÂY ===
            // Tham số boolean (multipart) phải đứng TRƯỚC String (encoding)
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, 
                    true,     // <-- 1. multipart (boolean)
                    "UTF-8"   // <-- 2. Mã hóa (String)
            );
            // === KẾT THÚC SỬA ===

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
}