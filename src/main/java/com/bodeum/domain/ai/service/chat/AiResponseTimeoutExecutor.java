package com.bodeum.domain.ai.service.chat;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * AI 답변 생성 작업에 전체 시간 제한을 적용하고,
 * 제한 시간을 넘긴 작업을 중단한다.
 */
@Component
public class AiResponseTimeoutExecutor {

    private final AsyncTaskExecutor executor;
    private final Duration timeout;

    public AiResponseTimeoutExecutor(
            @Qualifier("aiResponseExecutor") AsyncTaskExecutor executor,
            @Value("${bodeum.ai.response.timeout:90s}") Duration timeout
    ) {
        this.executor = executor;
        this.timeout = timeout == null || timeout.isZero() || timeout.isNegative()
                ? Duration.ofSeconds(90) : timeout;
    }

    public <T> T execute(Callable<T> task) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT, e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, cause);
        }
    }
}
