package com.bodeum.domain.ai.service.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AiResponseTimeoutExecutorTest {

    private ThreadPoolTaskExecutor taskExecutor;

    @AfterEach
    void tearDown() {
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
    }

    @Test
    void cancelsTaskAndReturnsTimeoutErrorAfterOverallDeadline() throws Exception {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.initialize();
        AiResponseTimeoutExecutor executor = new AiResponseTimeoutExecutor(
                taskExecutor, Duration.ofMillis(50));
        CountDownLatch interrupted = new CountDownLatch(1);

        assertThatThrownBy(() -> executor.execute(() -> {
            try {
                Thread.sleep(5_000);
                return "late answer";
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
            }
        }))
                .isInstanceOf(ProjectException.class)
                .extracting(error -> ((ProjectException) error).getErrorCode())
                .isEqualTo(AiErrorCode.AI_RESPONSE_TIMEOUT);

        org.assertj.core.api.Assertions.assertThat(
                interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }
}
