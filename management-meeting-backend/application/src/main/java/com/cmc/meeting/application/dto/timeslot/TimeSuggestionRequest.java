package com.cmc.meeting.application.dto.timeslot;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TimeSuggestionRequest {

    @NotEmpty(message = "Phải có ít nhất 1 người tham dự")
    private Set<Long> participantIds; // Danh sách ID của những người cần họp

    @NotNull
    @Future
    private LocalDateTime rangeStart; // Tìm từ thời điểm

    @NotNull
    @Future
    private LocalDateTime rangeEnd; // Tìm đến thời điểm

    @Min(value = 15, message = "Thời lượng họp phải ít nhất 15 phút")
    private int durationMinutes; // Thời lượng mong muốn (tính bằng phút)
}