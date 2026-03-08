package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDtoForRequestService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.UserEventSearchParams;
import ru.practicum.event.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private static final String USER_ID_HEADER = "X-EWM-USER-ID";
    private final EventService eventService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{eventId}")
    public EventFullDto publicSearchOne(@PathVariable Long eventId,
                                                        @RequestHeader(value = USER_ID_HEADER, required = false) Long userId,
                                                        HttpServletRequest request) {
        log.debug("Метод publicSearchOne(); eventId={}", eventId);

        EventFullDto event = eventService.getPublicBy(eventId, userId, request);
        return event;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<EventFullDto> publicSearchMany(@Valid @ModelAttribute UserEventSearchParams params,
                                                               HttpServletRequest request) {
        log.debug("Метод publicSearchMany(); {}", params);

        List<EventFullDto> events = eventService.getPublicBy(params, request);
        return events;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/internal/{eventId}")
    public EventDtoForRequestService getEventById(
            @PathVariable Long eventId) {

        log.debug("Feign-запрос: получение EventDtoForRequestService для eventId={}", eventId);

        EventDtoForRequestService dto = eventService.getEventDtoForRequestService(eventId);

        return dto;
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("internal/{eventId}/increment-confirmed")
    public EventDtoForRequestService incrementConfirmedRequests(
            @PathVariable Long eventId) {

        log.debug("Feign-запрос: инкремент confirmedRequests для eventId={}", eventId);

        try {
            EventDtoForRequestService updatedDto = eventService.incrementConfirmedRequests(eventId);
            return updatedDto;
        } catch (Exception e) {
            throw new InternalException("Ошибка при инкременте confirmedRequests для eventId={}: {}\", eventId, e.getMessage()");
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{eventId}/like")
    public void like(@PathVariable @PositiveOrZero @NotNull Long eventId,
                                     @RequestHeader(USER_ID_HEADER) Long userId) {
        log.debug("Метод like(); eventId={}, userId={}", eventId, userId);

        eventService.like(eventId, userId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "10") @Positive int size
    ) {
        log.debug("Метод getRecommendations();  userId={}, size={}", userId, size);

        List<EventShortDto> result = eventService.getRecommendations(userId, size);
        return result;
    }
}