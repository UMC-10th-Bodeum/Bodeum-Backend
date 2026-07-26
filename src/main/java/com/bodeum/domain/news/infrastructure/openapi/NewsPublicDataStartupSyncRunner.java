package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.service.NewsPublicDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "bodeum.news.public-data",
        name = "sync-on-startup",
        havingValue = "true"
)
public class NewsPublicDataStartupSyncRunner implements ApplicationRunner {

    private final NewsPublicDataSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        NewsPublicDataSyncService.NewsSyncResult result = syncService.sync();
        log.info(
                "News 공공데이터 동기화 완료: fetched={}, created={}, updated={}",
                result.fetched(),
                result.created(),
                result.updated()
        );
    }
}
