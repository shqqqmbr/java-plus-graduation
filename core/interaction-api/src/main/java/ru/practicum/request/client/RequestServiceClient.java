package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestServiceClient {
    @GetMapping("/by-ids")
    List<ParticipationRequestDto> getRequestsById(@RequestParam List<Long> requestIds);

    @GetMapping("/events/{eventId}")
    List<ParticipationRequestDto> getEventRequests(
            @RequestParam("userId") Long userId,
            @PathVariable("eventId") Long eventId);

    @PutMapping("/status")
    List<ParticipationRequestDto> updateRequestStatus(
            @RequestBody EventRequestStatusUpdateRequest request);

}
