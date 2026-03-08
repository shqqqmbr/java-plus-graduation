package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.client.EventServiceClient;
import ru.practicum.event.dto.EventDtoForRequestService;
import ru.practicum.event.dto.UpdRequestStatus;
import ru.practicum.event.enums.EventState;
import ru.practicum.ewm.client.CollectorClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.enums.RequestStatus;
import ru.practicum.user.client.UserServiceClient;
import ru.practicum.user.dto.UserDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final RequestRepository requestRepository;
    private final CollectorClient grpcCollectorClient;

    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        UserDto userDto = findUserBy(userId);
        EventDtoForRequestService eventDto = findEventBy(eventId);

        if (eventDto.getInitiatorId().equals(userId)) {
            throw new ConflictException("Нельзя участвовать в собственном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request уже создан ранее");
        }

        if (!eventDto.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long limit = eventDto.getParticipantLimit();
        long confirm = eventDto.getConfirmedRequests();

        if (limit > 0 && confirm >= limit) {
            throw new ConflictException("Достигнут лимит запросов на участие в событии");
        }

        RequestStatus status =
                (!eventDto.getRequestModeration() || limit == 0) ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        if (status == RequestStatus.CONFIRMED) {
            EventDtoForRequestService updatedEvent = eventServiceClient.incrementConfirmedRequests(eventId);

            if (updatedEvent == null) {
                throw new NotFoundException(
                        "Не удалось увеличить confirmedRequests: сервис event-service недоступен");
            }
        }

        Request request = Request.builder()
                .requesterId(userDto.getId())
                .eventId(eventId)
                .status(status)
                .build();
        request = requestRepository.save(request);

        grpcCollectorClient.recordRegister(userId, eventId);

        return requestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getAllBy(Long userId) {
        List<Request> result = requestRepository.findAllByRequesterId(userId);

        return result.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        this.findUserBy(userId);
        Request request = this.findRequestBy(requestId);
        request.setStatus(RequestStatus.CANCELED);

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User id={} не является автором этого запроса", userId);
        }
        request = requestRepository.save(request);

        return requestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        List<Request> requests = requestRepository.findAllByEventId(eventId);

        List<ParticipationRequestDto> dtos = requests.stream()
                .map(requestMapper::toDto)
                .toList();
        return dtos;
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds) {
        Set<Long> idSet = new HashSet<>(requestIds);
        return requestRepository.findAllByIdIn(idSet).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> updateRequestStatuses(Set<Long> requestIds, UpdRequestStatus newStatus) {
        List<Request> requests = requestRepository.findAllByIdIn(requestIds);

        RequestStatus status = toRequestStatus(newStatus);
        requests.forEach(request -> request.setStatus(status));

        requestRepository.saveAll(requests);

        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isParticipant(Long userId, Long eventId) {
        return requestRepository.existsByEventIdAndRequesterId(userId, eventId);
    }

    private RequestStatus toRequestStatus(UpdRequestStatus updStatus) {
        return switch (updStatus) {
            case CONFIRMED -> RequestStatus.CONFIRMED;
            case REJECTED  -> RequestStatus.REJECTED;
        };
    }


    private UserDto findUserBy(Long userId) {
        UserDto userDto = userServiceClient.getUserById(userId);
        if (userDto == null) {
            throw new NotFoundException("User id={}, не существует", userId);
        }
        return userDto;
    }

    private Request findRequestBy(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request id={} не найден", requestId));
    }

    private EventDtoForRequestService findEventBy(Long eventId) {
        EventDtoForRequestService eventDto = eventServiceClient.getEventById(eventId);
        if (eventDto == null) {
            throw new NotFoundException("Event id={} не найден", eventId);
        }
        return eventDto;
    }
}