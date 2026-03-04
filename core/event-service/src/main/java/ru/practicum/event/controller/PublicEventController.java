package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDtoForRequestService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UserEventSearchParams;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> publicSearchOne(@PathVariable @Positive Long eventId,
                                                        HttpServletRequest request) {
        EventFullDto event = eventService.getPublicBy(eventId, request);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<List<EventFullDto>> publicSearchMany(@Valid @ModelAttribute UserEventSearchParams params,
                                                               HttpServletRequest request) {
        List<EventFullDto> events = eventService.getPublicBy(params, request);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/internal/{eventId}")
    public ResponseEntity<EventDtoForRequestService> getEventById(
            @PathVariable Long eventId) {
        EventDtoForRequestService dto = eventService.getEventDtoForRequestService(eventId);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("internal/{eventId}/increment-confirmed")
    public ResponseEntity<EventDtoForRequestService> incrementConfirmedRequests(
            @PathVariable Long eventId) {
        try {
            EventDtoForRequestService updatedDto = eventService.incrementConfirmedRequests(eventId);
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}