package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityServiceImpl implements SimilarityService {

    private final Map<Long, Map<Long, Double>> eventWeights = new HashMap<>();
    private final Map<Long, Double> eventSummaryWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventMinSummaryWeights = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction) {
        long userId = userAction.getUserId();
        long eventId = userAction.getEventId();

        Map<Long, Double> userWeights = eventWeights.computeIfAbsent(eventId, e -> new HashMap<>());
        double oldWeight = userWeights.getOrDefault(userId, 0.0);
        double newWeight = getWeightByActionType(userAction.getActionType());

        if (oldWeight >= newWeight) {
            return List.of();
        }

        userWeights.merge(userId, newWeight, Math::max);

        double oldSum = eventSummaryWeights.getOrDefault(eventId, 0.0);
        double newSum = oldSum - oldWeight + newWeight;
        eventSummaryWeights.put(eventId, newSum);

        List<EventSimilarityAvro> eventSimilarityAvros = new ArrayList<>();

        for (long otherEventId : eventWeights.keySet()) {
            if (otherEventId == eventId ||
                    eventWeights.get(otherEventId) == null ||
                    !eventWeights.get(otherEventId).containsKey(userId)) {
                continue;
            }
            double newSumMinPairWeight = updateMinWeightSum(eventId, otherEventId, userId, oldWeight, newWeight);
            double similarity = calcSimilarity(eventId, otherEventId, newSumMinPairWeight);
            eventSimilarityAvros.add(getEventSimilarityAvro(eventId, otherEventId, similarity, userAction.getTimestamp()));
        }
        return eventSimilarityAvros;
    }

    private void putEventMinSummaryWeights(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        eventMinSummaryWeights
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    private double getEventMinSummaryWeights(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return eventMinSummaryWeights
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private double getWeightByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }


    private double updateMinWeightSum(long eventId, long otherEventId, long userId, double oldWeight, double newWeight) {
        double oldWeightOtherEvent = eventWeights.get(otherEventId).get(userId);

        double oldMinPairWeight = Math.min(oldWeight, oldWeightOtherEvent);
        double newMinPairWeight = Math.min(newWeight, oldWeightOtherEvent);

        long firstEventId = Math.min(eventId, otherEventId);
        long secondEventId = Math.max(eventId, otherEventId);

        double oldSumMinPairWeight = getEventMinSummaryWeights(firstEventId, secondEventId);

        if (oldMinPairWeight == newMinPairWeight) return oldSumMinPairWeight;

        double newSumMinPairWeight = oldSumMinPairWeight - oldMinPairWeight + newMinPairWeight;
        putEventMinSummaryWeights(firstEventId, secondEventId, newSumMinPairWeight);
        return newSumMinPairWeight;
    }

    private double calcSimilarity(long eventId, long otherEventId, double newSumMinPairWeight) {
        if (newSumMinPairWeight == 0) return 0;

        double sumEventWeight = eventSummaryWeights.get(eventId);
        double sumOtherEventWeight = eventSummaryWeights.get(otherEventId);
        return newSumMinPairWeight / (Math.sqrt(sumEventWeight) * Math.sqrt(sumOtherEventWeight));
    }

    private EventSimilarityAvro getEventSimilarityAvro(long eventId, long otherEventId, double similarity,
                                                       java.time.Instant timestamp) {
        long firstEventId = Math.min(eventId, otherEventId);
        long secondEventId = Math.max(eventId, otherEventId);

        return EventSimilarityAvro.newBuilder()
                .setEventA(firstEventId)
                .setEventB(secondEventId)
                .setTimestamp(timestamp)
                .setScore(similarity).build();
    }
}