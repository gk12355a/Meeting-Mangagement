package com.cmc.meeting.application.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

// DTO này tương tự DTO tạo mới, nhưng mọi trường đều là tùy chọn
// (Chúng ta sẽ dùng PATCH, nhưng giờ hãy làm PUT cho đơn giản)
@Data
public class MeetingUpdateRequest {

    @NotBlank(message = "Tiêu đề cuộc họp không được để trống")
    private String title;

    private String description;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;

    @NotNull
    private Long roomId;

    @NotNull
    @Size(min = 1)
    private Set<Long> participantIds;
}