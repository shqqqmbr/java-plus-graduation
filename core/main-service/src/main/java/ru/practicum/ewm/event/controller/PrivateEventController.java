package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

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
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable("userId") @NotNull @Positive Long userId,
                                               @RequestBody @Valid final NewEventDto newDto) {
        log.debug("Метод create(); userId = {}; newDto = {}", userId, newDto);

        EventFullDto result = eventService.create(userId, newDto);
        return result;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> findAll(@PathVariable("userId") @Positive Long userId,
                                                       @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                       @RequestParam(defaultValue = "10") @Positive int size) {
        log.debug("Метод findAll(); userId={}, from={}, size={}", userId, from, size);

        List<EventShortDto> result = eventService.getAllByUser(userId, from, size);
        return result;
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto find(@PathVariable("userId") @Positive Long userId,
                                             @PathVariable("eventId") @Positive Long eventId) {
        log.debug("Метод find(); userId={}, eventId={}", userId, eventId);

        EventFullDto result = eventService.getByUser(userId, eventId);
        return result;
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto update(@PathVariable("userId") @Positive Long userId,
                                               @PathVariable("eventId") @Positive Long eventId,
                                               @RequestBody @Valid final UpdEventUserRequest updDto) {
        log.debug("Метод update(); userId={}, eventId={}, updDto={}", userId, eventId, updDto);

        EventFullDto result = eventService.updateByUser(userId, eventId, updDto);
        return result;
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId,
                                                                         @PathVariable @Positive Long eventId) {
        log.debug("Метод getUserRequests(); userId={}, eventId={}", userId, eventId);

        List<ParticipationRequestDto> result = eventService.getEventRequests(userId, eventId);
        return result;
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public UpdRequestsStatusResult updateRequests(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest updDto
    ) {
        log.debug("Метод updateRequest(); userId={}, eventId={}", userId, eventId);

        UpdRequestsStatusResult result = eventService.updateRequests(userId, eventId, updDto);
        return result;
    }
}