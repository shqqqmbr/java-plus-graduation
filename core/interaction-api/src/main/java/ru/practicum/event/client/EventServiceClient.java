package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import ru.practicum.event.dto.EventDtoForRequestService;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    @GetMapping("/events/internal/{eventId}")
    EventDtoForRequestService getEventById(@PathVariable Long eventId);

    @PutMapping("/events/internal/{eventId}/increment-confirmed")
    EventDtoForRequestService incrementConfirmedRequests(@PathVariable Long eventId);
}