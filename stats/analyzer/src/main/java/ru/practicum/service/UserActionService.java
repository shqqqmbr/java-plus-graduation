package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.UserAction;
import ru.practicum.repository.UserActionRepository;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserActionService {

    private final UserActionRepository repository;

    @Transactional
    public void addAction(UserActionAvro value) {
        Optional<UserAction> oldActionOpt = repository.findByUserIdAndEventId(value.getUserId(), value.getEventId());
        if (oldActionOpt.isEmpty()) {
            UserAction action = UserAction.builder()
                    .userId(value.getUserId())
                    .eventId(value.getEventId())
                    .rating(getRatingByActionType(value.getActionType()))
                    .timestamp(LocalDateTime.ofInstant(value.getTimestamp(), ZoneId.systemDefault()))
                    .build();
            repository.save(action);
        } else {
            UserAction oldAction = oldActionOpt.get();
            double oldRating = oldAction.getRating();
            double newRating = getRatingByActionType(value.getActionType());
            if (newRating >= oldRating) {
                oldAction.setRating(newRating);
                LocalDateTime oldTimestamp = oldAction.getTimestamp();
                if (oldTimestamp == null || oldTimestamp.isBefore(LocalDateTime.ofInstant(value.getTimestamp(), ZoneId.systemDefault()))) {
                    oldAction.setTimestamp(LocalDateTime.ofInstant(value.getTimestamp(), ZoneId.systemDefault()));
                }
                repository.save(oldAction);
            }
        }
    }

    private double getRatingByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    @Transactional(readOnly = true)
    public Set<Long> findByUserIdOrderByTimestampDesc(long userId, int maxResult) {
        Pageable pageable = PageRequest.of(0, maxResult);
        List<Long> eventIds = repository.findDistinctEventIdByUserIdOrderByTimestampDesc(userId, pageable);

        return new HashSet<>(eventIds);
    }

    @Transactional(readOnly = true)
    public Set<Long> findAllByUserIdAndEventIdIn(long userId, Set<Long> eventIds) {
        List<Long> result = repository.findDistinctEventIdByUserIdAndEventIdIn(userId, eventIds);
        return new HashSet<>(result);
    }

    @Transactional(readOnly = true)
    public List<UserAction> findAllByEventIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return repository.findAllByEventIdIn(eventIds).stream().toList();
    }
}