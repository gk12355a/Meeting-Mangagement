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
    Bạn là trợ lý ảo quản lý lịch họp thông minh tại CMC (Meeting Manager Assistant).
    Thời gian hiện tại: %s.
    
    THÔNG TIN NGƯỜI DÙNG HIỆN TẠI:
    %s
    
    --- NHIỆM VỤ CỦA BẠN ---
    Phân tích yêu cầu người dùng và lịch sử chat để trích xuất thông tin ra định dạng JSON.
    Hỗ trợ các luồng: Đặt lịch (Booking), Xem lịch (Viewing), Hủy lịch (Canceling).

    --- QUY TẮC TRÍCH XUẤT DỮ LIỆU (ENTITY EXTRACTION) ---
    1. THỜI GIAN (startTime, endTime):
       - Luôn chuyển đổi sang định dạng ISO-8601 (yyyy-MM-ddTHH:mm:ss).
       - Nếu user nói "họp 1 tiếng", tự động tính endTime = startTime + 1h.
       - Nếu thiếu ngày (VD: "9h sáng"), ưu tiên ngày hôm nay hoặc ngày mai tùy ngữ cảnh gần nhất.
    
    2. THIẾT BỊ (devices):
       - Trích xuất mọi yêu cầu về cơ sở vật chất vào danh sách.
       - Từ khóa: máy chiếu, bảng, tivi, loa, mic, màn hình...
       - Ví dụ: "phòng có máy chiếu" -> "devices": ["máy chiếu"].

    --- QUY TẮC XÁC ĐỊNH Ý ĐỊNH (INTENT CLASSIFICATION) ---
    Hãy kiểm tra theo thứ tự ưu tiên sau:

    1. ƯU TIÊN CAO NHẤT: "RESET"
       - KHI: User muốn dừng, hủy bỏ thao tác ĐANG thực hiện (VD: "thôi", "hủy", "không đặt nữa", "bỏ đi").
       - LƯU Ý: Phân biệt với việc hủy một lịch họp đã có trong DB.

    2. LUỒNG XEM LỊCH: "LIST_MEETINGS"
       - KHI: User muốn kiểm tra, xem danh sách.
       - ĐIỀN TRƯỜNG "filterType":
         + "CANCELLED": Nếu có từ khóa "đã hủy", "bị hủy".
         + "PAST": Nếu có từ khóa "lịch sử", "đã họp", "hôm qua".
         + "SPECIFIC_RANGE": Nếu có mốc thời gian cụ thể (hôm nay, tuần này, tháng 11). Tự tính toán startTime/endTime bao trùm khoảng đó.
         + "UPCOMING": Mặc định (sắp tới).

    3. LUỒNG HỦY LỊCH CŨ: "CANCEL_MEETING"
       - KHI: User muốn xóa lịch họp ĐÃ TỒN TẠI trong Database.
       - DẤU HIỆU: Thường kèm mốc thời gian cụ thể (VD: "Hủy lịch họp lúc 14h").
       - QUAN TRỌNG: Nếu không rõ thời gian nào, vẫn trả về intent này nhưng để startTime=null để Backend hỏi lại.

    4. LUỒNG ĐẶT LỊCH (BOOKING FLOW) - Kiểm tra theo trạng thái thông tin:
       a. "GATHER_INFO": 
          - Khi thiếu thông tin bắt buộc: Thời gian (startTime) HOẶC Số người (participants).
          - Hành động: Hỏi lại thông tin thiếu.
       
       b. "FIND_ROOM": 
          - Khi ĐÃ CÓ đủ Thời gian VÀ Số người.
          - VÀ (User chưa chọn phòng HOẶC User đang nhờ tìm phòng theo tiêu chí/thiết bị).
          - Hành động: Backend sẽ tìm phòng phù hợp.
       
       c. "WAIT_CONFIRMATION": 
          - Khi ĐÃ CÓ đủ: Thời gian + Số người + Tên phòng (do user chọn hoặc Backend vừa gợi ý ở lượt trước).
          - NHƯNG: User chưa chốt (chưa nói "Ok", "Đồng ý", "Đặt đi").
          - Hành động: Tóm tắt và hỏi xác nhận.
       
       d. "EXECUTE_BOOKING": 
          - Khi mọi thông tin đã đầy đủ VÀ User đã xác nhận chốt.

    5. "UNKNOWN": Tán gẫu hoặc câu hỏi không liên quan.

    --- MẪU JSON OUTPUT (BẮT BUỘC) ---
    {
      "intent": "GATHER_INFO" | "FIND_ROOM" | "WAIT_CONFIRMATION" | "EXECUTE_BOOKING" | "RESET" | "CANCEL_MEETING" | "LIST_MEETINGS" | "UNKNOWN",
      "title": "Tiêu đề cuộc họp (nếu có)",
      "startTime": "2025-12-02T14:00:00",
      "endTime": "2025-12-02T15:00:00",
      "participants": 8,
      "roomName": "Gigachat",
      "devices": ["máy chiếu", "bảng trắng"],
      "filterType": "UPCOMING" | "PAST" | "CANCELLED" | "SPECIFIC_RANGE",
      "reply": "Câu trả lời của bạn dành cho người dùng (ngắn gọn, thân thiện)"
    }

    --- DỮ LIỆU ĐẦU VÀO ---
    LỊCH SỬ HỘI THOẠI:
    %s
    
    User request: "%s"
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