package com.bodeum.domain.ai.infrastructure.config;

import com.bodeum.domain.ai.service.AiMessageFailureService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AiMessageProcessingRecoveryRunner {

    private final AiMessageFailureService failureService;

    @Value("${bodeum.ai.response-processing.stale-after:2m}")
    private Duration staleAfter;

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        int recovered = failureService.recoverStaleProcessingMessages(
                Instant.now().minus(staleAfter));
        if (recovered > 0) {
            log.warn("Recovered stale AI response requests: count={}", recovered);
        }
    }
}
