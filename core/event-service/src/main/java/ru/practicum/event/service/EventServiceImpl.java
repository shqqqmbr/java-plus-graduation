package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.ewm.client.CollectorClient;
import ru.practicum.ewm.client.RecommendationsClient;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.client.RequestServiceClient;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.enums.RequestStatus;
import ru.practicum.user.client.UserServiceClient;
import ru.practicum.user.dto.UserDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static java.time.ZoneOffset.UTC;
import static ru.practicum.event.enums.EventState.CANCELED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final UserServiceClient userServiceClient;
    private final EventRepository eventRepository;
    private final RequestServiceClient requestServiceClient;
    private final CategoryRepository categoryRepository;

    private final EventMapper eventMapper;

    private final CollectorClient grpcCollectorClient;
    private final RecommendationsClient grpcAnalyzerClient;

    // Private API:
    @Override
    @Transactional
    public EventFullDto create(Long userId, final NewEventDto newDto) {
        this.checkStartDate(newDto.getEventDate());
        UserDto userDto = userServiceClient.getUserById(userId);
        if (userDto == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        Category category = this.findCategoryBy(newDto.getCategory());

        Event event = eventMapper.toEntity(newDto);
        event.setLocation(newDto.getLocation());
        event.setInitiatorId(userDto.getId());
        event.setCategory(category);
        event = eventRepository.save(event);
        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, int from, int size) {

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return events.map(eventMapper::toShortDto).getContent();
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} у user id={} не найдено", eventId, userId));

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdEventUserRequest updDto) {
        this.checkEventDateForUpdate(updDto);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} не найдено; User id={} ", eventId, userId));

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event id={} нельзя изменить; его status={}", eventId, event.getState());
        }
        if (!(event.getState().equals(CANCELED) || event.getState().equals(EventState.PENDING))) {
            throw new ConflictException("Event id={} нельзя обновить пока оно опубликовано", eventId);
        }
        if (updDto.getCategory() != null) {
            event.setCategory(this.findCategoryBy(updDto.getCategory()));
        }

        if (updDto.getStateAction() != null) {
            switch (updDto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(CANCELED);
            }
        }
        eventMapper.updateFromDto(updDto, event);
        event = eventRepository.save(event);
        return eventMapper.toFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        List<ParticipationRequestDto> requests = requestServiceClient.getEventRequests(userId, eventId);

        return requests;
    }

    @Override
    @Transactional
    public UpdRequestsStatusResult updateRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest updDto) {
        Event event = this.findEventBy(eventId);
        List<ParticipationRequestDto> requestDtos = requestServiceClient.getRequestsByIds(
                updDto.getRequestIds().stream().toList()
        );

        if (requestDtos.isEmpty()) {
            return UpdRequestsStatusResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        UpdRequestsStatusResult result;

        switch (updDto.getStatus()) {
            case UpdRequestStatus.CONFIRMED -> {
                if (event.getConfirmedRequests() == event.getParticipantLimit().longValue()) {
                    throw new ConflictException("На Event id={} больше нет мест", eventId);
                }

                int availableSlots = event.getParticipantLimit() == 0
                        ? requestDtos.size()
                        : event.getParticipantLimit().intValue() - event.getConfirmedRequests().intValue();

                List<Long> toConfirmIds = requestDtos.stream()
                        .limit(availableSlots)
                        .map(ParticipationRequestDto::getId)
                        .toList();

                List<Long> toRejectIds = requestDtos.size() > availableSlots
                        ? requestDtos.stream()
                        .skip(availableSlots)
                        .map(ParticipationRequestDto::getId)
                        .toList()
                        : List.of();

                List<ParticipationRequestDto> confirmedDtos = List.of();
                List<ParticipationRequestDto> rejectedDtos = List.of();

                if (!toConfirmIds.isEmpty()) {
                    confirmedDtos = requestServiceClient.updateRequestStatuses(
                            EventRequestStatusUpdateRequest.builder()
                                    .requestIds(new HashSet<>(toConfirmIds))
                                    .status(UpdRequestStatus.CONFIRMED)
                                    .build()
                    );
                }

                if (!toRejectIds.isEmpty()) {
                    rejectedDtos = requestServiceClient.updateRequestStatuses(
                            EventRequestStatusUpdateRequest.builder()
                                    .requestIds(new HashSet<>(toRejectIds))
                                    .status(UpdRequestStatus.REJECTED)
                                    .build()
                    );
                }

                // Обновляем количество подтверждённых
                event.setConfirmedRequests(event.getConfirmedRequests() + confirmedDtos.size());
                eventRepository.save(event);

                result = UpdRequestsStatusResult.builder()
                        .confirmedRequests(confirmedDtos)
                        .rejectedRequests(rejectedDtos)
                        .build();
            }

            case UpdRequestStatus.REJECTED -> {
                if (requestDtos.stream().anyMatch(dto ->
                        dto.getStatus() == RequestStatus.CONFIRMED)) {
                    throw new ConflictException("Нельзя отклонить подтверждённые заявки");
                }

                List<ParticipationRequestDto> rejectedDtos = requestServiceClient.updateRequestStatuses(
                        EventRequestStatusUpdateRequest.builder()
                                .requestIds(updDto.getRequestIds())
                                .status(UpdRequestStatus.REJECTED)
                                .build()
                );

                result = UpdRequestsStatusResult.builder()
                        .confirmedRequests(List.of())
                        .rejectedRequests(rejectedDtos)
                        .build();
            }

            default -> throw new IllegalArgumentException("Неизвестный статус: " + updDto.getStatus());
        }

        return result;
    }


    // Admin API:
    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdEventAdminRequest updDto) {
        Event event = this.findEventBy(eventId);

        eventMapper.updateFromDto(updDto, event);

        this.checkEventDateForPublish(updDto.getEventDate());

        if (updDto.getStateAction() != null) {
            switch (updDto.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(EventState.PUBLISHED);
                        event.setPublishedOn(Instant.now());
                    } else if (event.getState().equals(CANCELED) ||
                            event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Event id={} нельзя опубликовать; его status={}",
                                eventId, event.getState());
                    }
                }
                case REJECT_EVENT -> {
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(CANCELED);
                    } else if (event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Опубликованные Event не могут быть отклонены");
                    }
                }
            }
        }

        event = eventRepository.save(event);
        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> searchForAdmin(AdminEventSearchParams params) {
        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            conditions.add(event.initiatorId.in(params.getUsers()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            conditions.add(event.state.in(params.getStates()));
        }

        if (params.getRangeStart() != null) {
            Instant rangeStart = params.getRangeStart().atZone(UTC).toInstant();
            conditions.add(event.eventDate.after(rangeStart));
        }

        if (params.getRangeEnd() != null) {
            Instant rangeEnd = params.getRangeEnd().atZone(UTC).toInstant();
            conditions.add(event.eventDate.before(rangeEnd));
        }

        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(Expressions.TRUE);

        int page = params.getFrom() / params.getSize();
        Pageable pageable = PageRequest.of(page, params.getSize());

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        return events.map(eventMapper::toFullDto).getContent();
    }


    // Public API:
    @Override
    public EventFullDto getPublicBy(Long eventId, Long userId, HttpServletRequest request) {

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованного Event id={} нет", eventId));

        grpcCollectorClient.recordView(userId, eventId);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> getPublicBy(UserEventSearchParams params, HttpServletRequest request) {

        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        conditions.add(event.state.eq(EventState.PUBLISHED));

        if (params.getText() != null && !params.getText().isEmpty()) {
            conditions.add(
                    event.annotation.containsIgnoreCase(params.getText())
                            .or(event.description.containsIgnoreCase(params.getText())));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }

        if (params.getPaid() != null) {
            conditions.add(event.paid.eq(params.getPaid()));
        }

        if (params.getRangeStart() != null) {
            Instant rangeStart = params.getRangeStart().atZone(UTC).toInstant();
            conditions.add(event.eventDate.after(rangeStart));
        }

        if (params.getRangeEnd() != null) {
            Instant rangeEnd = params.getRangeEnd().atZone(UTC).toInstant();
            conditions.add(event.eventDate.before(rangeEnd));
        }

        if (params.getRangeStart() == null && params.getRangeEnd() == null) {
            conditions.add(event.eventDate.after(Instant.now()));
        }

        if (params.getOnlyAvailable() != null) {
            conditions.add(event.confirmedRequests.lt(event.participantLimit.longValue()));
        }

        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(Expressions.TRUE);

        int page = params.getFrom() / params.getSize();

        Pageable pageable = null;

        switch (params.getSort()) {
            case EVENT_DATE -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.ASC, "eventDate"));
            case RATING -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.DESC, "views"));
        }

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        return events.map(eventMapper::toFullDto).getContent();
    }

    @Override
    public void like(Long userId, Long eventId) {
        if (!requestServiceClient.isParticipant(userId, eventId)) {
            throw new BadRequestException("User id={} не является участником event eventId={}", userId, eventId);
        }

        if (!eventRepository.existsByIdAndEventDateBefore(eventId, Instant.now())) {
            throw new BadRequestException("Event eventId={} еще не прошло", eventId);
        }

        grpcCollectorClient.recordLike(userId, eventId);
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        Map<Long, Double> scoreByEvent = grpcAnalyzerClient.getRecommendationsForUser(userId, maxResults);
        Set<Long> eventIds = scoreByEvent.keySet();
        List<Event> events = eventRepository.findAllByIdIn(eventIds);

        return events.stream()
                .map(eventMapper::toShortDto)
                .peek(dto -> dto.setRating(scoreByEvent.get(dto.getId())))
                .toList();
    }

    @Override
    @Transactional
    public EventDtoForRequestService incrementConfirmedRequests(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        eventRepository.save(event);
        return eventMapper.toEventDtoForRequestService(event);
    }

    @Override
    public EventDtoForRequestService getEventDtoForRequestService(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        return eventMapper.toEventDtoForRequestService(event);
    }

    private Category findCategoryBy(Long categoryId) {

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Объект Category id={} не найден", categoryId));
    }

    private Event findEventBy(Long eventId) {

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Объект Event id={} не найден", eventId));
    }

    private void checkStartDate(LocalDateTime eventDate) {

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата Event при СОЗДАНИИ должна быть в будущем, мин. через 2 часа");
        }
    }

    private void checkEventDateForUpdate(UpdEventUserRequest updDto) {

        if (updDto.getEventDate() != null) {
            this.checkStartDate(updDto.getEventDate());
        }
    }

    private void checkEventDateForPublish(LocalDateTime eventDate) {

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата Event при ПУБЛИКАЦИИ должна быть в будущем, мин. через 1 час");
        }
    }
}