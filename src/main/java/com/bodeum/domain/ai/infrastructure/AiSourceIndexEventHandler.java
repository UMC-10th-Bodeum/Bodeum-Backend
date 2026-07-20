package com.bodeum.domain.ai.infrastructure;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.AiSourceChangedEvent;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.repository.NewsRepository;
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

    private final InfoItemRepository infoItemRepository;
    private final NewsRepository newsRepository;
    private final AiContentIndexingService indexingService;

    @Async("aiIndexingExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(AiSourceChangedEvent event) {
        try {
            if (event.changeType() == AiSourceChangedEvent.ChangeType.DELETE) {
                indexingService.delete(event.sourceType(), event.sourceId());
                return;
            }
            if (event.sourceType() == AiResponseSourceType.INFO) {
                infoItemRepository.findById(event.sourceId())
                        .ifPresentOrElse(
                                indexingService::replaceInfo,
                                () -> indexingService.delete(event.sourceType(), event.sourceId())
                        );
                return;
            }
            newsRepository.findIndexableById(event.sourceId())
                    .ifPresentOrElse(
                            indexingService::replaceNews,
                            () -> indexingService.delete(event.sourceType(), event.sourceId())
                    );
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
