package com.cmc.meeting.application.dto.recurrence;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RecurrenceRuleDTO {
    @NotNull
    private FrequencyType frequency; // Lặp HÀNG NGÀY, TUẦN, THÁNG

    @NotNull
    @Min(1)
    private Integer interval = 1; // Lặp "cách" (every) 1 (ngày/tuần/tháng)

    @NotNull
    @Future
    private LocalDate repeatUntil; // Lặp cho đến ngày
}