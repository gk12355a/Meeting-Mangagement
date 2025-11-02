package com.cmc.meeting.infrastructure.notification;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.Map;

@Service
public class ThymeleafEmailService {

    private final SpringTemplateEngine emailTemplateEngine;

    public ThymeleafEmailService(SpringTemplateEngine emailTemplateEngine) {
        this.emailTemplateEngine = emailTemplateEngine;
    }

    /**
     * "Vẽ" file HTML
     * @param templateName Tên file (vd: "email-template.html")
     * @param variables Map chứa các biến (vd: {"title", "Họp... "})
     * @return Một chuỗi String chứa HTML hoàn chỉnh
     */
    public String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return emailTemplateEngine.process(templateName, context);
    }
}