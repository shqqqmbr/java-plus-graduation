package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;

@FeignClient(name = "event-service")
@Component
public interface EventServiceClient {

    @GetMapping("/events/internal/{eventId}")
    EventShortDto getEventById(@PathVariable Long eventId);

    @GetMapping("/events/internal/{eventId}")
    EventFullDto getEventByIdFull(@PathVariable Long eventId);

    @PutMapping("/events/internal/{eventId}/increment-confirmed")
    EventShortDto incrementConfirmedRequests(@PathVariable Long eventId);
}