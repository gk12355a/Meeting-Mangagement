package com.cmc.meeting.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

import com.cmc.meeting.application.dto.recurrence.RecurrenceRuleDTO;

@Data
public class MeetingCreationRequest {

    @NotBlank(message = "Tiêu đề cuộc họp không được để trống")
    @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @Future(message = "Thời gian bắt đầu phải ở tương lai")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @Future(message = "Thời gian kết thúc phải ở tương lai")
    private LocalDateTime endTime;

    @NotNull(message = "Phải chọn phòng họp")
    private Long roomId;

    @NotNull(message = "Phải có ít nhất 1 người tham dự")
    @Size(min = 1, message = "Phải mời ít nhất 1 người")
    private Set<Long> participantIds; 

    // BỔ SUNG TRƯỜNG CÒN THIẾU
    private Set<Long> deviceIds;
    // BỔ SUNG: (US-3)
    @Valid // Validate lồng
    private RecurrenceRuleDTO recurrenceRule; // (Nếu = null, là họp 1 lần)
}