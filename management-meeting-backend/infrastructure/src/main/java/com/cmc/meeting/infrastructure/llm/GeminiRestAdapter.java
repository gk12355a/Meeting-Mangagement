package com.cmc.meeting.infrastructure.llm;

import com.cmc.meeting.application.dto.chat.StructuredIntent;
import com.cmc.meeting.application.port.llm.LanguageModelPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class GeminiRestAdapter implements LanguageModelPort {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final ObjectMapper objectMapper;

    // Hardcode URL chuẩn của Google để tránh sai sót do ghép chuỗi .env
    // Nếu bước 1 bạn thấy model khác, hãy sửa tên model ở đây
    // Sửa dòng này trong GeminiRestAdapter.java
private static final String GOOGLE_API_URL ="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public GeminiRestAdapter(@Value("${gemini.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public StructuredIntent getStructuredIntent(String query) {
        try {
            // 1. Setup Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Setup Body (Prompt)
            String systemInstruction = String.format("""
                Bạn là trợ lý ảo quản lý lịch họp thông minh. Thời gian hiện tại là: %s.
                
                NHIỆM VỤ: 
                Phân tích yêu cầu của người dùng và trích xuất thông tin ra định dạng JSON.
                
                CÁC INTENT (Ý ĐỊNH) CẦN NHẬN DIỆN:
                
                1. "SCHEDULE_MEETING": Người dùng muốn đặt phòng hoặc tạo lịch họp mới.
                   - Yêu cầu trích xuất: title (tiêu đề), startTime, endTime, roomName, participants (số người).
                   - Nếu user nói "họp 1 tiếng", hãy tự tính endTime = startTime + 1 giờ.
                   
                2. "LIST_MEETINGS": Người dùng muốn xem, kiểm tra danh sách lịch họp của họ.
                   - Ví dụ: "Lịch của tôi hôm nay", "Mai có cuộc họp nào không", "Xem danh sách họp".
                   - Yêu cầu trích xuất: startTime (nếu user hỏi ngày cụ thể).
                   
                3. "CANCEL_MEETING": Người dùng muốn hủy một cuộc họp.
                   - Ví dụ: "Hủy cuộc họp lúc 2 giờ", "Xóa lịch họp team".
                   - Yêu cầu trích xuất: startTime (để xác định cuộc họp nào cần hủy), cancelReason (lý do hủy).
                   
                4. "UNKNOWN": Câu chào hỏi (Hi, Hello) hoặc câu hỏi không liên quan đến đặt phòng.
                
                QUY TẮC QUAN TRỌNG (BẮT BUỘC TUÂN THỦ):
                - KHÔNG trả về Markdown (không dùng ```json). Chỉ trả về Raw JSON.
                - Định dạng ngày giờ: BẮT BUỘC là ISO-8601 (yyyy-MM-ddTHH:mm:ss).
                - Nếu user không nói rõ ngày (ví dụ "9 giờ sáng"), hãy dùng ngày hiện tại hoặc ngày mai tùy ngữ cảnh gần nhất.
                
                MẪU JSON TRẢ VỀ:
                {
                  "intent": "SCHEDULE_MEETING" | "LIST_MEETINGS" | "CANCEL_MEETING" | "UNKNOWN",
                  "title": "Tiêu đề họp",
                  "roomName": "Tên phòng (ví dụ: Phòng A, Phòng VIP)",
                  "startTime": "2023-11-25T14:00:00",
                  "endTime": "2023-11-25T15:00:00",
                  "participants": 5,
                  "cancelReason": "Lý do hủy (nếu có)",
                  "reply": "Câu trả lời ngắn gọn, thân thiện nếu intent là UNKNOWN"
                }
                
                User request: %s
                """, LocalDateTime.now(), query);

            GeminiRequest requestBody = new GeminiRequest();
            requestBody.setContents(List.of(new Content(List.of(new Part(systemInstruction)))));

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);

            // 3. Gọi API (Thêm key vào URL)
            String finalUrl = GOOGLE_API_URL + "?key=" + apiKey;
            log.info("Sending request to Google AI: {}", GOOGLE_API_URL); // Log URL gốc (giấu key)

            ResponseEntity<String> response = restTemplate.exchange(
                    finalUrl, HttpMethod.POST, entity, String.class);

            // 4. Xử lý kết quả
            String jsonResponse = response.getBody();
            log.info("Google Response: {}", jsonResponse); // In ra response để debug

            GeminiResponse geminiResponse = objectMapper.readValue(jsonResponse, GeminiResponse.class);
            
            if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
                return makeErrorIntent("Google không trả về dữ liệu (Candidates empty).");
            }

            String rawText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            String cleanJson = cleanJson(rawText);
            
            return objectMapper.readValue(cleanJson, StructuredIntent.class);

        } catch (HttpClientErrorException e) {
            // Bắt lỗi 4xx (400, 404, 403...) và IN RA MÀN HÌNH
            log.error("Lỗi HTTP từ Google: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return makeErrorIntent("Lỗi HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return makeErrorIntent("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private StructuredIntent makeErrorIntent(String msg) {
        StructuredIntent intent = new StructuredIntent();
        intent.setIntent("UNKNOWN");
        intent.setReply(msg);
        return intent;
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        if (text.startsWith("```json")) return text.substring(7, text.length() - 3).trim();
        if (text.startsWith("```")) return text.substring(3, text.length() - 3).trim();
        return text;
    }

    // Inner Classes
    @Data static class GeminiRequest { private List<Content> contents; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) static class GeminiResponse { private List<Candidate> candidates; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) static class Candidate { private Content content; }
    @Data @AllArgsConstructor @NoArgsConstructor static class Content { private List<Part> parts; }
    @Data @AllArgsConstructor @NoArgsConstructor static class Part { private String text; }
}