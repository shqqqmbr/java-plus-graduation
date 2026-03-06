package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto publicSearchOne(@PathVariable @Positive Long eventId,
                                                        HttpServletRequest request) {
        EventFullDto event = eventService.getPublicBy(eventId, request);
        return event;
    }

    @GetMapping
    public List<EventFullDto> publicSearchMany(@Valid @ModelAttribute UserEventSearchParams params,
                                                               HttpServletRequest request) {
        List<EventFullDto> events = eventService.getPublicBy(params, request);
        return events;
    }

    @GetMapping("/internal/{eventId}")
    public EventDtoForRequestService getEventById(
            @PathVariable Long eventId) {
        EventDtoForRequestService dto = eventService.getEventDtoForRequestService(eventId);

        return dto;
    }

    @PutMapping("/internal/{eventId}/increment-confirmed")
    public EventDtoForRequestService incrementConfirmedRequests(
            @PathVariable Long eventId) {
        EventDtoForRequestService updatedDto = eventService.incrementConfirmedRequests(eventId);
        return updatedDto;
    }
}