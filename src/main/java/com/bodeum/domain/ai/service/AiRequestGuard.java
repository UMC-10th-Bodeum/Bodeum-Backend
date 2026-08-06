package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRequestGuard {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final AiMessageRepository aiMessageRepository;
    private final int perMinuteLimit;
    private final int dailyLimit;
    private final ZoneId dailyLimitZone;
    private final Map<Long, RequestState> states = new ConcurrentHashMap<>();

    public AiRequestGuard(
            AiMessageRepository aiMessageRepository,
            @Value("${bodeum.ai.request-limit.per-minute:5}") int perMinuteLimit,
            @Value("${bodeum.ai.request-limit.per-day:50}") int dailyLimit,
            @Value("${bodeum.ai.request-limit.time-zone:Asia/Seoul}") String timeZone
    ) {
        this.aiMessageRepository = aiMessageRepository;
        this.perMinuteLimit = perMinuteLimit;
        this.dailyLimit = dailyLimit;
        this.dailyLimitZone = ZoneId.of(timeZone);
    }

    public Permit acquire(Long userId, Long chatRoomId) {
        Instant now = Instant.now();
        validateDailyLimit(chatRoomId, now);

        RequestState state;
        while (true) {
            state = states.computeIfAbsent(userId, ignored -> new RequestState());
            synchronized (state) {
                // cleanup이 Map에서 제거하기로 결정한 상태라면 해당 객체를 사용하지 않는다.
                if (state.removed) {
                    continue;
                }
                removeExpiredRequests(state, now);
                if (state.inProgress) {
                    throw new ProjectException(AiErrorCode.AI_REQUEST_IN_PROGRESS);
                }
                if (state.acceptedAt.size() >= perMinuteLimit) {
                    throw new ProjectException(AiErrorCode.AI_RATE_LIMIT_EXCEEDED);
                }
                state.acceptedAt.addLast(now);
                state.inProgress = true;
                state.lastAccessedAt = now;
                break;
            }
        }
        cleanupInactiveStates(now);
        RequestState acquiredState = state;
        return () -> release(acquiredState);
    }

    private void validateDailyLimit(Long chatRoomId, Instant now) {
        LocalDate today = now.atZone(dailyLimitZone).toLocalDate();
        Instant startAt = today.atStartOfDay(dailyLimitZone).toInstant();
        Instant endAt = today.plusDays(1).atStartOfDay(dailyLimitZone).toInstant();
        long used = aiMessageRepository
                .countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        chatRoomId, SenderType.USER, startAt, endAt);
        if (used >= dailyLimit) {
            throw new ProjectException(AiErrorCode.AI_DAILY_LIMIT_EXCEEDED);
        }
    }

    private void release(RequestState state) {
        synchronized (state) {
            state.inProgress = false;
            state.lastAccessedAt = Instant.now();
        }
    }

    private void removeExpiredRequests(RequestState state, Instant now) {
        Instant cutoff = now.minus(ONE_MINUTE);
        while (!state.acceptedAt.isEmpty() && !state.acceptedAt.getFirst().isAfter(cutoff)) {
            state.acceptedAt.removeFirst();
        }
    }

    private void cleanupInactiveStates(Instant now) {
        if (states.size() < 1_000) {
            return;
        }
        Instant cutoff = now.minus(ONE_MINUTE);
        states.entrySet().removeIf(entry -> {
            RequestState state = entry.getValue();
            synchronized (state) {
                if (!state.inProgress && state.lastAccessedAt.isBefore(cutoff)) {
                    state.removed = true;
                    return true;
                }
                return false;
            }
        });
    }

    private static final class RequestState {
        private final ArrayDeque<Instant> acceptedAt = new ArrayDeque<>();
        private boolean inProgress;
        private boolean removed;
        private Instant lastAccessedAt = Instant.EPOCH;
    }

    @FunctionalInterface
    public interface Permit extends AutoCloseable {
        // try-with-resources 종료 시 진행 중 상태가 반드시 해제되도록 하는 용도이다.
        @Override
        void close();
    }
}
