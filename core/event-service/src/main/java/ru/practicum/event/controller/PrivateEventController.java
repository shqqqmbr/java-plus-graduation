package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.net.URI;
import java.util.List;


@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable("userId") @NotNull @Positive Long userId,
                                               @RequestBody @Valid final NewEventDto newDto) {

        EventFullDto result = eventService.create(userId, newDto);
        return result;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> findAll(@PathVariable("userId") @Positive Long userId,
                                                       @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                       @RequestParam(defaultValue = "10") @Positive int size) {

        List<EventShortDto> result = eventService.getAllByUser(userId, from, size);
        return result;
    }

    @GetMapping("/{eventId}")
    public EventFullDto find(@PathVariable("userId") @Positive Long userId,
                                             @PathVariable("eventId") @Positive Long eventId) {

        EventFullDto result = eventService.getByUser(userId, eventId);
        return result;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable("userId") @Positive Long userId,
                                               @PathVariable("eventId") @Positive Long eventId,
                                               @RequestBody @Valid final UpdEventUserRequest updDto) {

        EventFullDto result = eventService.updateByUser(userId, eventId, updDto);
        return result;
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId,
                                                                         @PathVariable @Positive Long eventId) {

        List<ParticipationRequestDto> result = eventService.getEventRequests(userId, eventId);
        return result;
    }

    @PatchMapping("/{eventId}/requests")
    public UpdRequestsStatusResult updateRequests(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest updDto
    ) {

        UpdRequestsStatusResult result = eventService.updateRequests(userId, eventId, updDto);
        return result;
    }
}