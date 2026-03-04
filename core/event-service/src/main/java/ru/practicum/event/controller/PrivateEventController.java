package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.net.URI;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventFullDto> create(@PathVariable("userId") @NotNull @Positive Long userId,
                                               @RequestBody @Valid final NewEventDto newDto) {

        EventFullDto result = eventService.create(userId, newDto);
        return ResponseEntity
                .created(URI.create("/events/" + result.getId()))
                .body(result);
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> findAll(@PathVariable("userId") @Positive Long userId,
                                                       @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                       @RequestParam(defaultValue = "10") @Positive int size) {

        List<EventShortDto> result = eventService.getAllByUser(userId, from, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> find(@PathVariable("userId") @Positive Long userId,
                                             @PathVariable("eventId") @Positive Long eventId) {

        EventFullDto result = eventService.getByUser(userId, eventId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> update(@PathVariable("userId") @Positive Long userId,
                                               @PathVariable("eventId") @Positive Long eventId,
                                               @RequestBody @Valid final UpdEventUserRequest updDto) {

        EventFullDto result = eventService.updateByUser(userId, eventId, updDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable @Positive Long userId,
                                                                         @PathVariable @Positive Long eventId) {

        List<ParticipationRequestDto> result = eventService.getEventRequests(userId, eventId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{eventId}/requests")
    public ResponseEntity<UpdRequestsStatusResult> updateRequests(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest updDto
    ) {

        UpdRequestsStatusResult result = eventService.updateRequests(userId, eventId, updDto);
        return ResponseEntity.ok(result);
    }
}