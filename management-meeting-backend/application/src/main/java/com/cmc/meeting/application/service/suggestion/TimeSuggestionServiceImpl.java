package com.cmc.meeting.application.service.suggestion;

import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO;
import com.cmc.meeting.application.dto.timeslot.TimeSuggestionRequest;
import com.cmc.meeting.application.port.service.TimeSuggestionService;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TimeSuggestionServiceImpl implements TimeSuggestionService {

    private final MeetingRepository meetingRepository;

    public TimeSuggestionServiceImpl(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @Override
    public List<TimeSlotDTO> suggestTime(TimeSuggestionRequest request) {

        // 1. Lấy tất cả các cuộc họp "BẬN" của nhóm người này
        List<Meeting> busyMeetings = meetingRepository.findMeetingsForUsersInDateRange(
                request.getParticipantIds(), 
                request.getRangeStart(), 
                request.getRangeEnd()
        );

        // 2. Chuyển đổi sang các "Slot Bận" (TimeSlotDTO)
        List<TimeSlotDTO> busySlots = busyMeetings.stream()
                .map(m -> new TimeSlotDTO(m.getStartTime(), m.getEndTime()))
                .sorted(Comparator.comparing(TimeSlotDTO::getStartTime)) // Sắp xếp
                .collect(Collectors.toList());

        // 3. Gộp các "Slot Bận" bị chồng chéo
        List<TimeSlotDTO> mergedBusySlots = mergeOverlappingSlots(busySlots);

        // 4. "Đảo ngược" (Invert) để tìm các "Slot Trống"
        return findFreeSlots(
                mergedBusySlots, 
                request.getRangeStart(), 
                request.getRangeEnd(), 
                request.getDurationMinutes()
        );
    }

    /**
     * Thuật toán Gộp các khoảng thời gian bận
     * (Ví dụ: [9:00-10:00] và [9:30-10:30] -> [9:00-10:30])
     */
    private List<TimeSlotDTO> mergeOverlappingSlots(List<TimeSlotDTO> busySlots) {
        if (busySlots.isEmpty()) {
            return busySlots;
        }

        LinkedList<TimeSlotDTO> merged = new LinkedList<>();
        merged.add(busySlots.get(0));

        for (int i = 1; i < busySlots.size(); i++) {
            TimeSlotDTO current = busySlots.get(i);
            TimeSlotDTO last = merged.getLast();

            // Nếu slot hiện tại chồng lên slot cuối cùng
            if (current.getStartTime().isBefore(last.getEndTime())) {
                // Mở rộng slot cuối cùng
                last.setEndTime(
                    current.getEndTime().isAfter(last.getEndTime()) ? 
                    current.getEndTime() : 
                    last.getEndTime()
                );
            } else {
                merged.add(current);
            }
        }
        return merged;
    }

    /**
     * Thuật toán Đảo ngược (Invert) để tìm slot trống
     */
    private List<TimeSlotDTO> findFreeSlots(List<TimeSlotDTO> mergedBusySlots, 
                                            LocalDateTime rangeStart, 
                                            LocalDateTime rangeEnd, 
                                            int durationMinutes) {

        List<TimeSlotDTO> freeSlots = new ArrayList<>();
        LocalDateTime currentFreeStart = rangeStart;

        // Lặp qua các slot bận đã gộp
        for (TimeSlotDTO busySlot : mergedBusySlots) {
            if (currentFreeStart.isBefore(busySlot.getStartTime())) {
                // Tìm thấy 1 slot trống (từ currentFreeStart -> busySlot.getStartTime)
                addIfSlotIsLongEnough(
                    freeSlots, 
                    currentFreeStart, 
                    busySlot.getStartTime(), 
                    durationMinutes);
            }
            // Cập nhật điểm bắt đầu trống tiếp theo
            currentFreeStart = busySlot.getEndTime();
        }

        // Kiểm tra slot trống cuối cùng (từ slot bận cuối -> rangeEnd)
        if (currentFreeStart.isBefore(rangeEnd)) {
            addIfSlotIsLongEnough(
                freeSlots, 
                currentFreeStart, 
                rangeEnd, 
                durationMinutes);
        }

        return freeSlots;
    }

    /**
     * Helper: Chỉ thêm slot nếu nó đủ dài
     */
    private void addIfSlotIsLongEnough(List<TimeSlotDTO> freeSlots, 
                                       LocalDateTime start, 
                                       LocalDateTime end, 
                                       int durationMinutes) {

        if (java.time.Duration.between(start, end).toMinutes() >= durationMinutes) {
            freeSlots.add(new TimeSlotDTO(start, end));
        }
    }
}