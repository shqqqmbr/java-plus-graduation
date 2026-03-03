package ru.practicum.service;

import ru.practicum.event.dto.UpdRequestStatus;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Set;

public interface RequestService {

    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> getAllBy(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds);

    List<ParticipationRequestDto> updateRequestStatuses(Set<Long> requestIds, UpdRequestStatus status);
}