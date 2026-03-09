package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzerService {

    private final UserActionService userActionService;
    private final EventSimilarityService eventSimilarityService;

    public Iterable<ru.practicum.stats.proto.RecommendedEventProto> getRecommendationsForUser(ru.practicum.stats.proto.UserPredictionsRequestProto request) {
        Set<Long> actionIds = userActionService.findByUserIdOrderByTimestampDesc(request.getUserId(),
                request.getMaxResults());

        List<EventSimilarity> similarities = eventSimilarityService.findNPairContainsEventIdsSortedDescScore(actionIds,
                request.getMaxResults());

        Map<Long, Double> eventIds = similarities.stream()
                .collect(Collectors.toMap(o -> actionIds.contains(o.getEvent1()) ? o.getEvent2() : o.getEvent1(),
                        EventSimilarity::getSimilarity, Double::max));

        return eventIds.entrySet().stream().map(o -> ru.practicum.stats.proto.RecommendedEventProto.newBuilder()
                .setEventId(o.getKey())
                .setScore(o.getValue())
                .build()).toList();
    }

    public Iterable<ru.practicum.stats.proto.RecommendedEventProto> getSimilarEvents(ru.practicum.stats.proto.SimilarEventsRequestProto request) {
        List<EventSimilarity> similarPair = eventSimilarityService.findAllContainsEventId(request.getEventId());

        Set<Long> ids = similarPair.stream().map(EventSimilarity::getEvent1).collect(Collectors.toSet());
        Set<Long> otherIds = similarPair.stream().map(EventSimilarity::getEvent2).collect(Collectors.toSet());
        ids.addAll(otherIds);

        Set<Long> userEventIds = userActionService.findAllByUserIdAndEventIdIn(request.getUserId(), ids);

        similarPair.removeIf(o -> userEventIds.contains(o.getEvent1()) && userEventIds.contains(o.getEvent2()));

        return similarPair.stream()
                .sorted(Comparator.comparing(EventSimilarity::getSimilarity, Comparator.reverseOrder()))
                .limit(request.getMaxResults())
                .map(o -> {
                    long eventId = Objects.equals(o.getEvent1(), request.getEventId())
                            ? o.getEvent2()
                            : o.getEvent1();
                    return ru.practicum.stats.proto.RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(o.getSimilarity())
                            .build();
                }).toList();
    }

    public List<ru.practicum.stats.proto.RecommendedEventProto> getInteractionsCount(ru.practicum.stats.proto.InteractionsCountRequestProto request) {
        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        List<UserAction> userActions = userActionService.findAllByEventIds(eventIds);

        Map<Long, Double> actionMap = userActions.stream()
                .collect(Collectors.groupingBy(
                        UserAction::getEventId,
                        Collectors.summingDouble(UserAction::getRating)));

        return actionMap.entrySet().stream()
                .map(o -> ru.practicum.stats.proto.RecommendedEventProto.newBuilder()
                    .setEventId(o.getKey())
                    .setScore(o.getValue())
                    .build())
                .toList();
    }
}