package com.cmc.meeting.infrastructure.notification;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationAdapter implements EmailNotificationPort {

    private final JavaMailSender javaMailSender;

    public EmailNotificationAdapter(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
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
}