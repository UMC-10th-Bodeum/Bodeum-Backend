package com.bodeum.domain.ai.infrastructure.indexing;

import com.bodeum.domain.ai.model.indexing.AiIndexingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "bodeum.ai.indexing.run-once",
        havingValue = "true"
)
public class AiIndexingRunOnceRunner {

    private final AiContentIndexingService indexingService;
    private final ConfigurableApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        AiIndexingResult result = indexingService.rebuildAll();
        log.info(
                "AI vector indexing completed: infoSources={}, newsSources={}, documents={}",
                result.infoSourceCount(),
                result.newsSourceCount(),
                result.documentCount()
        );
        SpringApplication.exit(applicationContext, () -> 0);
    }
}
