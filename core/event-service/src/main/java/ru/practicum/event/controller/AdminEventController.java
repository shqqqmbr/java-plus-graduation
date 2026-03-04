package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.AdminEventSearchParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdEventAdminRequest;
import ru.practicum.event.service.EventService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> adminUpdate(@PathVariable @Positive Long eventId,
                                                    @RequestBody @Valid UpdEventAdminRequest updDto) {

        EventFullDto eventFullDto = eventService.updateByAdmin(eventId, updDto);
        return ResponseEntity.ok(eventFullDto);
    }

    @GetMapping
    public ResponseEntity<List<EventFullDto>> adminSearch(@Valid @ModelAttribute AdminEventSearchParams params) {

        List<EventFullDto> events = eventService.searchForAdmin(params);
        return ResponseEntity.ok(events);
    }
}