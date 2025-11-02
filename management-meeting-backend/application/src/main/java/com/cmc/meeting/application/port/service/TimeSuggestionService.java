package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO;
import com.cmc.meeting.application.dto.timeslot.TimeSuggestionRequest;
import java.util.List;

public interface TimeSuggestionService {
    List<TimeSlotDTO> suggestTime(TimeSuggestionRequest request);
}