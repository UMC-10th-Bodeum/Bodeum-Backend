package com.bodeum.domain.ai.infrastructure.indexing;

import com.bodeum.domain.ai.model.indexing.AiSourceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AiSourceIndexEventHandler {

    private final AiContentIndexingService indexingService;

    @Async("aiIndexingExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(AiSourceChangedEvent event) {
        try {
            indexingService.synchronize(event.sourceType(), event.sourceId());
        } catch (Exception e) {
            log.error(
                    "AI source indexing failed after commit: sourceType={}, sourceId={}",
                    event.sourceType(),
                    event.sourceId(),
                    e
            );
        }
    }
}
