package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/events/{eventId}")
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @RequestParam("userId") Long userId,
            @PathVariable("eventId") Long eventId) {
        List<ParticipationRequestDto> requests = requestService.getEventRequests(userId, eventId);

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<ParticipationRequestDto>> getRequestsByIds(
            @RequestParam List<Long> requestIds) {

        List<ParticipationRequestDto> dtos = requestService.getRequestsByIds(requestIds);
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/status")
    public ResponseEntity<List<ParticipationRequestDto>> updateRequestStatuses(
            @RequestBody EventRequestStatusUpdateRequest request) {

        List<ParticipationRequestDto> updatedDtos = requestService.updateRequestStatuses(
                request.getRequestIds(),
                request.getStatus()
        );
        return ResponseEntity.ok(updatedDtos);
    }

    @PostMapping("{eventId}/participant/{userId}")
    public boolean isUserParticipant(@PathVariable Long userId, @PathVariable Long eventId) {
        return requestService.isParticipant(userId, eventId);
    }
}
