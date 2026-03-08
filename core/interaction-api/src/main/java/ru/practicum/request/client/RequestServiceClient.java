package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestServiceClient {

    @GetMapping("/events/{eventId}")
    List<ParticipationRequestDto> getEventRequests(
            @RequestParam("userId") Long userId,
            @PathVariable("eventId") Long eventId);

    @GetMapping("/by-ids")
    List<ParticipationRequestDto> getRequestsByIds(@RequestParam List<Long> requestIds);


    @PutMapping("/status")
    List<ParticipationRequestDto> updateRequestStatuses(
            @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("{eventId}/participant/{userId}")
    boolean isParticipant(@PathVariable Long userId, @PathVariable Long eventId);
}
