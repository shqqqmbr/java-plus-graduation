package ru.practicum.ewm.client;


import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.InteractionsCountRequestProto;
import ru.practicum.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.stats.proto.UserPredictionsRequestProto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RecommendationsClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public Map<Long, Double> getEventRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Map<Long, Double> ratings = new HashMap<>();
            client.getInteractionsCount(request).forEachRemaining(response -> {
                ratings.put(response.getEventId(), response.getScore());
            });

            log.info("Получены рейтинги для {} мероприятий", ratings.size());
            return ratings;
        } catch (StatusRuntimeException e) {
            log.error("Не удалось получить рейтинги мероприятий. Статус: {}, сообщение: {}",
                    e.getStatus(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Не удалось получить рейтинги мероприятий. Исключение: {}, сообщение: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении рейтингов мероприятий", e);
        }
    }

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public Map<Long, Double> getRecommendationsForUser(Long userId, Integer maxResults) {
        if (userId == null) {
            return Map.of();
        }

        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults != null ? maxResults : 10)
                    .build();

            Map<Long, Double> recommendations = new HashMap<>();
            client.getRecommendationsForUser(request).forEachRemaining(response -> {
                recommendations.put(response.getEventId(), response.getScore());
            });

            log.info("Получены рекомендации для пользователя {}: {} мероприятий", userId, recommendations.size());
            return recommendations;
        } catch (StatusRuntimeException e) {
            log.error("Не удалось получить рекомендации для пользователя {}. Статус: {}, сообщение: {}",
                    userId, e.getStatus(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Не удалось получить рекомендации для пользователя {}. Исключение: {}, сообщение: {}",
                    userId, e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении рекомендаций для пользователя", e);
        }
    }
}