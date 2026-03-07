package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.AdminEventSearchParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdEventAdminRequest;
import ru.practicum.event.service.EventService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    @PatchMapping("/{eventId}")
    public EventFullDto adminUpdate(@PathVariable @Positive Long eventId,
                                                    @RequestBody @Valid UpdEventAdminRequest updDto) {

        EventFullDto eventFullDto = eventService.updateByAdmin(eventId, updDto);
        return eventFullDto;
    }

    @GetMapping
    public List<EventFullDto> adminSearch(@Valid @ModelAttribute AdminEventSearchParams params) {

        List<EventFullDto> events = eventService.searchForAdmin(params);
        return events   ;
    }
}