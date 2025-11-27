package com.cmc.meeting.infrastructure.llm;

import com.cmc.meeting.application.dto.chat.ChatMessage;
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
import java.util.List;

@Component
@Slf4j
public class GeminiRestAdapter implements LanguageModelPort {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final ObjectMapper objectMapper;

    // Hardcode URL model Flash cho nhanh và rẻ
    private static final String GOOGLE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public GeminiRestAdapter(@Value("${gemini.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public StructuredIntent getStructuredIntent(String query, List<ChatMessage> history, String userContextInfo) {
        try {
            // 1. Setup Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Xử lý Lịch sử (Convert List -> String text)
            StringBuilder historyContext = new StringBuilder();
            if (history != null && !history.isEmpty()) {
                historyContext.append("\n=== BẮT ĐẦU LỊCH SỬ HỘI THOẠI (Context) ===\n");
                for (ChatMessage msg : history) {
                    // Clean nội dung để tránh lỗi JSON
                    String cleanContent = msg.getContent() != null ? msg.getContent().replace("\"", "'") : "";
                    historyContext.append(String.format("- %s: %s\n", msg.getRole().toUpperCase(), cleanContent));
                }
                historyContext.append("=== KẾT THÚC LỊCH SỬ ===\n");
            } else {
                historyContext.append("(Chưa có lịch sử hội thoại)");
            }

            // 3. Setup Body (Prompt Engineering)
            // Đây là nơi ta "giới thiệu" người dùng với AI
            String systemInstruction = String.format("""
    Bạn là trợ lý ảo đặt phòng họp thông minh. Thời gian hiện tại: %s.
    
    THÔNG TIN NGƯỜI DÙNG: %s
    
    NHIỆM VỤ CỦA BẠN:
    Hỗ trợ người dùng đặt lịch họp qua các bước: Thu thập thông tin -> Chọn phòng -> Xác nhận -> Đặt.
    
    HÃY PHÂN TÍCH LỊCH SỬ CHAT VÀ TRẢ VỀ JSON THEO QUY TẮC SAU:
    
    1. INTENT: "GATHER_INFO"
       - KHI: Người dùng muốn đặt lịch nhưng THIẾU thời gian (startTime) HOẶC số lượng người (participants).
       - HÀNH ĐỘNG: Hỏi lại thông tin còn thiếu một cách tự nhiên.
       
    2. INTENT: "FIND_ROOM"
       - KHI: Đã có đủ thời gian và số người, nhưng người dùng CHƯA chốt tên phòng cụ thể.
       - HÀNH ĐỘNG: Trả về startTime, endTime, participants để backend tìm phòng phù hợp.
       
    3. INTENT: "WAIT_CONFIRMATION"
       - KHI: Đã có đủ: Thời gian + Số người + Tên phòng (do user chọn hoặc bot gợi ý trước đó).
       - NHƯNG: Người dùng CHƯA nói các từ khóa xác nhận như "Ok", "Đồng ý", "Chốt", "Đặt đi".
       - HÀNH ĐỘNG: Tóm tắt lại thông tin và hỏi "Bạn có muốn chốt lịch này không?".
       
    4. INTENT: "EXECUTE_BOOKING"
       - KHI: Đã đủ mọi thông tin VÀ người dùng đã nói "Ok/Đồng ý/Yes".
       
    5. "RESET" (Hủy thao tác hiện tại):
       - KHI: Người dùng nói "Không", "Hủy", "Thôi", "Bỏ đi", "Cancel" KHI ĐANG TRONG QUÁ TRÌNH đặt phòng (đang hỏi giờ, đang chọn phòng, chờ xác nhận).
       - HÀNH ĐỘNG: Dừng lại việc đặt phòng. Trả về reply: "Đã hủy thao tác."
       
    6. "CANCEL_MEETING" (Hủy lịch trong DB):
       - KHI: Người dùng muốn xóa một lịch họp ĐÃ ĐẶT THÀNH CÔNG trước đó.
       - DẤU HIỆU: Thường đi kèm mốc thời gian cụ thể (ví dụ: "Hủy lịch họp lúc 14h", "Xóa lịch chiều nay").
       - NẾU KHÔNG CÓ THỜI GIAN: Hãy trả về Intent này nhưng để startTime = null để code hỏi lại.
       
    7. "FIND_ROOM" (Tìm phòng):
       - LƯU Ý ĐẶC BIỆT: Nếu người dùng vừa nói "Không" hoặc "Chưa chốt", ĐỪNG trả về intent này nữa. Hãy chuyển sang "RESET" hoặc hỏi người dùng muốn thay đổi gì.
    8. "LIST_MEETINGS" (Xem lịch):
       - KHI: Người dùng muốn xem các lịch họp đã đặt.
       - DẤU HIỆU: Các từ khóa như "Xem lịch họp", "Lịch của tôi", "Nhắc tôi về các cuộc họp".
       - HÀNH ĐỘNG: Trả về danh sách lịch hợp
    9. "UNKNOWN" (Tán gẫu).

    MẪU JSON OUTPUT:
    {
      "intent": "GATHER_INFO" | "FIND_ROOM" | "WAIT_CONFIRMATION" | "EXECUTE_BOOKING",
      "title": "Tiêu đề (nếu có)",
      "startTime": "ISO-8601",
      "endTime": "ISO-8601",
      "participants": 5,
      "roomName": "Tên phòng (nếu có)",
      "reply": "Câu trả lời của bạn"
    }

    LỊCH SỬ HỘI THOẠI:
    %s
    
    User request: %s
    """, 
    LocalDateTime.now(), 
    userContextInfo, 
    historyContext.toString(), 
    query);

            GeminiRequest requestBody = new GeminiRequest();
            requestBody.setContents(List.of(new Content(List.of(new Part(systemInstruction)))));

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);

            // 4. Gọi API
            String finalUrl = GOOGLE_API_URL + "?key=" + apiKey;
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.POST, entity, String.class);

            // 5. Xử lý kết quả
            String jsonResponse = response.getBody();
            GeminiResponse geminiResponse = objectMapper.readValue(jsonResponse, GeminiResponse.class);

            if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
                return makeErrorIntent("Google AI không phản hồi.");
            }

            String rawText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            String cleanJson = cleanJson(rawText);

            return objectMapper.readValue(cleanJson, StructuredIntent.class);

        } catch (HttpClientErrorException e) {
            log.error("Google AI Error: {}", e.getResponseBodyAsString());
            return makeErrorIntent("Lỗi kết nối AI: " + e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return makeErrorIntent("Lỗi hệ thống xử lý tin nhắn.");
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

    // Inner Classes (Giữ nguyên)
    @Data static class GeminiRequest { private List<Content> contents; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) static class GeminiResponse { private List<Candidate> candidates; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) static class Candidate { private Content content; }
    @Data @AllArgsConstructor @NoArgsConstructor static class Content { private List<Part> parts; }
    @Data @AllArgsConstructor @NoArgsConstructor static class Part { private String text; }
}