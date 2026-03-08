package ru.practicum.ewm.client;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Service
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordView(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordRegister(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordLike(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendAction(Long userId, Long eventId, ActionTypeProto actionType) {
        try {
            Instant now = Instant.now();

            UserActionProto userAction = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();

            client.collectUserAction(userAction);
            log.info("Действие пользователя успешно отправлено: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);
        } catch (StatusRuntimeException e) {
            log.error("Не удалось отправить действие пользователя. Статус: {}, сообщение: {}",
                    e.getStatus(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Не удалось отправить действие пользователя. Исключение: {}, сообщение: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке действия пользователя", e);
        }
    }
}