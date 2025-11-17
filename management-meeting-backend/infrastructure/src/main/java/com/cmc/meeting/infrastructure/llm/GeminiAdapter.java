package com.cmc.meeting.infrastructure.llm;

import com.cmc.meeting.application.port.llm.LanguageModelPort;
import com.cmc.meeting.application.dto.chat.StructuredIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeminiAdapter implements LanguageModelPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper; // Để parse JSON text

    @Value("${gemini.api.key}")
    private String apiKey;

    // Ví dụ: "https://generativelanguage.googleapis.com"
    @Value("${gemini.api.baseurl}") 
    private String baseUrl;

    // Ví dụ: "/v1beta/models/gemini-pro:generateContent"
    @Value("${gemini.api.endpoint}")
    private String endpoint;

    public GeminiAdapter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        // Cấu hình WebClient (Bạn có thể cần cấu hình base URL ở đây)
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public StructuredIntent getStructuredIntent(String query) {
        String prompt = buildSystemPrompt(query);
        
        // TODO: Xây dựng request body (ví dụ: GeminiRequest)
        // Cấu trúc body này phụ thuộc vào API của Gemini
        // Ví dụ: new GeminiRequest(List.of(new Content(List.of(new Part(prompt)))))
        Object geminiRequestBody = buildGeminiRequest(prompt); 

        try {
            // 1. Gọi API
            String jsonResponse = webClient.post()
                    .uri(baseUrl + endpoint + "?key=" + apiKey)
                    .bodyValue(geminiRequestBody)
                    .retrieve()
                    .bodyToMono(String.class) // Lấy text thô về
                    .block(); // Đơn giản hóa

            // 2. TODO: Parse text thô (jsonResponse) để lấy ra phần text JSON
            // mà bạn yêu cầu trong prompt.
            // Phản hồi của Gemini có thể là một JSON lớn,
            // bạn cần trích xuất phần "text" chứa JSON của bạn.
            String intentJsonText = extractJsonFromResponse(jsonResponse);

            // 3. Parse JSON text đó thành DTO
            return objectMapper.readValue(intentJsonText, StructuredIntent.class);

        } catch (Exception e) {
            // Nếu parse lỗi, trả về intent UNKNOWN
            e.printStackTrace(); // Nên dùng Logger
            StructuredIntent errorIntent = new StructuredIntent();
            errorIntent.setIntent("UNKNOWN");
            errorIntent.setReply("Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu của bạn với AI.");
            return errorIntent;
        }
    }
    
    // --- CÁC METHOD HỖ TRỢ ---

    private Object buildGeminiRequest(String prompt) {
        // TODO: Tạo đối tượng request theo đúng cấu trúc API của Gemini
        // Đây là ví dụ, bạn cần kiểm tra lại tài liệu
        // return new GeminiRequest(prompt); 
        return null; // Placeholder
    }

    private String extractJsonFromResponse(String rawApiResponse) {
        // TODO: Viết logic để trích xuất chuỗi JSON từ phản hồi của Gemini
        // (ví dụ: lấy từ response.candidates[0].content.parts[0].text)
        // và dọn dẹp (loại bỏ ```json ... ``` nếu có)
        return rawApiResponse; // Placeholder
    }

    private String buildSystemPrompt(String query) {
        // Đây là trái tim của adapter, bạn cần tinh chỉnh kỹ
        return "Bạn là một trợ lý đặt lịch họp. Nhiệm vụ của bạn là phân tích văn bản của người dùng và trả lời BẰNG MỘT ĐỐI TƯỢNG JSON DUY NHẤT, KHÔNG CÓ BẤT CỨ CHỮ GÌ KHÁC. " +
               "Ngày giờ hiện tại là: " + java.time.LocalDateTime.now() + ".\n" +
               "Các intent hỗ trợ: 'SCHEDULE_MEETING', 'SUGGEST_ROOM', 'UNKNOWN'.\n" +
               "Các trường dữ liệu: intent (String), participants (Integer), startTime (LocalDateTime), endTime (LocalDateTime), title (String), reply (String).\n" +
               "Luôn trả về startTime và endTime ở định dạng ISO (ví dụ: '2025-11-18T15:00:00').\n" +
               "Ví dụ 1:\n" +
               "User: 'đặt phòng 10 người 3h chiều mai'\n" +
               "JSON: { \"intent\": \"SCHEDULE_MEETING\", \"participants\": 10, \"startTime\": \"2025-11-18T15:00:00\", \"endTime\": \"2025-11-18T16:00:00\" }\n" + // Giả sử họp 1 tiếng
               "Ví dụ 2:\n" +
               "User: 'tìm phòng 5 người 10h sáng nay'\n" +
               "JSON: { \"intent\": \"SUGGEST_ROOM\", \"participants\": 5, \"startTime\": \"2025-11-17T10:00:00\" }\n" +
               "Ví dụ 3:\n" +
               "User: 'thời tiết thế nào?'\n" +
               "JSON: { \"intent\": \"UNKNOWN\", \"reply\": \"Tôi chỉ có thể giúp bạn về việc đặt phòng họp.\" }\n" +
               "\n" +
               "Hãy phân tích câu sau đây:\n" +
               "User: '" + query + "'\n" +
               "JSON: ";
    }
}