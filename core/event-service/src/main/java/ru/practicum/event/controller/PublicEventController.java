package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDtoForRequestService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UserEventSearchParams;
import ru.practicum.event.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> publicSearchOne(@PathVariable @Positive Long eventId,
                                                        HttpServletRequest request) {
        log.debug("Метод publicSearchOne(); eventId={}", eventId);

        EventFullDto event = eventService.getPublicBy(eventId, request);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<List<EventFullDto>> publicSearchMany(@Valid @ModelAttribute UserEventSearchParams params,
                                                               HttpServletRequest request) {
        log.debug("Метод publicSearchMany(); {}", params);

        List<EventFullDto> events = eventService.getPublicBy(params, request);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/internal/{eventId}")
    public ResponseEntity<EventDtoForRequestService> getEventById(
            @PathVariable Long eventId) {

        log.debug("Feign-запрос: получение EventDtoForRequestService для eventId={}", eventId);

        EventDtoForRequestService dto = eventService.getEventDtoForRequestService(eventId);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("internal/{eventId}/increment-confirmed")
    public ResponseEntity<EventDtoForRequestService> incrementConfirmedRequests(
            @PathVariable Long eventId) {

        log.debug("Feign-запрос: инкремент confirmedRequests для eventId={}", eventId);

        try {
            EventDtoForRequestService updatedDto = eventService.incrementConfirmedRequests(eventId);
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            log.error("Ошибка при инкременте confirmedRequests для eventId={}: {}", eventId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}