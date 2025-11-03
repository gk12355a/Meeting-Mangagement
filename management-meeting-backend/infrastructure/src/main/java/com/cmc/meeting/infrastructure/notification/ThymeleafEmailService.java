package com.cmc.meeting.infrastructure.notification;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.cmc.meeting.application.port.service.AppConfigService;

import java.util.Map;

@Service
public class ThymeleafEmailService {

    private final SpringTemplateEngine emailTemplateEngine;
    private final AppConfigService appConfigService;

    public ThymeleafEmailService(SpringTemplateEngine emailTemplateEngine, 
                                 AppConfigService appConfigService) {
        this.emailTemplateEngine = emailTemplateEngine;
        this.appConfigService = appConfigService;
    }

    /**
     * "Vẽ" file HTML từ KEY trong CSDL
     * @param templateKey Tên key (vd: "email.template.internal")
     * @param variables Map chứa các biến
     * @return Chuỗi HTML hoàn chỉnh
     */
    public String processTemplate(String templateKey, Map<String, Object> variables) {
        
        // BƯỚC 1: Lấy template (HTML) từ CSDL
        String htmlTemplate = appConfigService.getValue(
            templateKey, 
            "<html><body>Lỗi: Không tìm thấy template.</body></html>" // Default
        );
        
        // BƯỚC 2: "Vẽ"
        Context context = new Context();
        context.setVariables(variables);
        return emailTemplateEngine.process(htmlTemplate, context);
    }
}