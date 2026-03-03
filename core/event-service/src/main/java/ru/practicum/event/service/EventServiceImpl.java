package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.practicum.ewm.ReqStatsParams;
import ru.practicum.ewm.StatsDto;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.client.RequestServiceClient;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.enums.RequestStatus;
import ru.practicum.user.client.UserServiceClient;
import ru.practicum.user.dto.UserDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static ru.practicum.event.enums.EventState.CANCELED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final UserServiceClient userServiceClient;
    private final EventRepository eventRepository;
    private final RequestServiceClient requestServiceClient;
    private final CategoryRepository categoryRepository;

    private final EventMapper eventMapper;

    private final StatsClient statsClient;

    // Private API:
    @Override
    @Transactional
    public EventFullDto create(Long userId, final NewEventDto newDto) {
        log.debug("Метод create(); userId={}, newDto={}", userId, newDto);

        this.checkStartDate(newDto.getEventDate());
        UserDto userDto = userServiceClient.getUserById(userId);
        if (userDto == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        Category category = this.findCategoryBy(newDto.getCategory());

        Event event = eventMapper.toEntity(newDto);
        event.setLocation(newDto.getLocation());
        event.setInitiator(userDto.getId());
        event.setCategory(category);
        event = eventRepository.save(event);

        log.debug("Создан event={}", event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, int from, int size) {
        log.debug("Метод getAllByUser(); userId={}", userId);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        Page<Event> events = eventRepository.findAllByInitiator(userId, pageable);

        return events.map(eventMapper::toShortDto).getContent();
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        log.debug("Метод getByUser(); eventId={}, userId={}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiator(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} у user id={} не найдено", eventId, userId));

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdEventUserRequest updDto) {
        log.debug("Метод userUpdate(); userId={}, eventId: {}, dto={}",
                userId, eventId, updDto);

        this.checkEventDateForUpdate(updDto);

        Event event = eventRepository.findByIdAndInitiator(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} не найдено; User id={} ", eventId, userId));

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event id={} нельзя изменить; его status={}", eventId, event.getState());
        }

        log.debug("Найден Event в репозитории; event={}", event);

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

        log.debug("Метод userUpdate(); Event обновлен в репозитории event={}", event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.debug("Метод getEventRequests(); userId={}, eventId={}", userId, eventId);

        List<ParticipationRequestDto> requests = requestServiceClient.getEventRequests(userId, eventId);

        return requests;
    }

    @Override
    @Transactional
    public UpdRequestsStatusResult updateRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest updDto) {
        log.debug("Метод updateRequests(), userId={}, eventId={}", userId, eventId);


        Event event = this.findEventBy(eventId);
        List<ParticipationRequestDto> requestDtos = requestServiceClient.getRequestsById(
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
                    confirmedDtos = requestServiceClient.updateRequestStatus(
                            EventRequestStatusUpdateRequest.builder()
                                    .requestIds(new HashSet<>(toConfirmIds))
                                    .status(UpdRequestStatus.CONFIRMED)
                                    .build()
                    );
                }

                if (!toRejectIds.isEmpty()) {
                    rejectedDtos = requestServiceClient.updateRequestStatus(
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

                List<ParticipationRequestDto> rejectedDtos = requestServiceClient.updateRequestStatus(
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
        log.debug("Метод adminUpdateEvent(); eventId: {}, dto={}", eventId, updDto);

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

                    log.debug("Для Event назначен статус={}, время публикации publishedOn={}",
                            event.getState(), event.getPublishedOn());
                }
                case REJECT_EVENT -> {
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(CANCELED);
                    } else if (event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Опубликованные Event не могут быть отклонены");
                    }

                    log.debug("Для Event назначен статус={}", event.getState());
                }
            }
        }

        event = eventRepository.save(event);

        log.debug("Метод adminUpdate(); Event обновлен в репозитории event={}", event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> searchForAdmin(AdminEventSearchParams params) {
        log.debug("Метод adminSearchEvents; {}", params);

        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            conditions.add(event.initiator.in(params.getUsers()));
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

        log.debug("{}", finalCondition);

        int page = params.getFrom() / params.getSize();
        Pageable pageable = PageRequest.of(page, params.getSize());

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        return events.map(eventMapper::toFullDto).getContent();
    }


    // Public API:
    @Override
    public EventFullDto getPublicBy(Long eventId, HttpServletRequest request) {
        log.debug("Метод getPublicById(); eventId={}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованного Event id={} нет", eventId));

        statsClient.hit(request);
        this.setViewsForEvent(event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> getPublicBy(UserEventSearchParams params, HttpServletRequest request) {
        log.debug("Метод publicSearchMany; {}", params);

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

        log.debug("{}", finalCondition);

        int page = params.getFrom() / params.getSize();

        Pageable pageable = null;

        switch (params.getSort()) {
            case EVENT_DATE -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.ASC, "eventDate"));
            case VIEWS -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.DESC, "views"));
        }

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        statsClient.hit(request);

        return events.map(eventMapper::toFullDto).getContent();
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
        log.debug("Поиск Category id={} в репозитории", categoryId);

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Объект Category id={} не найден", categoryId));
    }

    private Event findEventBy(Long eventId) {
        log.debug("Поиск Event id={} в репозитории", eventId);

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Объект Event id={} не найден", eventId));
    }

    private void checkStartDate(LocalDateTime eventDate) {
        log.debug("Проверка даты при СОЗДАНИИ");

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата Event при СОЗДАНИИ должна быть в будущем, мин. через 2 часа");
        }
    }

    private void checkEventDateForUpdate(UpdEventUserRequest updDto) {
        log.debug("Проверка даты Event при ОБНОВЛЕНИИ");

        if (updDto.getEventDate() != null) {
            this.checkStartDate(updDto.getEventDate());
        }
    }

    private void checkEventDateForPublish(LocalDateTime eventDate) {
        log.debug("Проверка даты Event при ПУБЛИКАЦИИ");

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата Event при ПУБЛИКАЦИИ должна быть в будущем, мин. через 1 час");
        }
    }

    private void setViewsForEvent(Event event) {
        List<StatsDto> stats = statsClient.getStats(ReqStatsParams.builder()
                .start(LocalDateTime.now().minusYears(100))
                .end(LocalDateTime.now().plusYears(1))
                .uris(List.of("/events/" + event.getId()))
                .unique(true)
                .build());

        event.setViews(stats.getFirst().getHits());
    }
}