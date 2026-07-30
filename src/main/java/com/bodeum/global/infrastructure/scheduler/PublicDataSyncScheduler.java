package com.bodeum.global.infrastructure.scheduler;

import com.bodeum.domain.info.service.publicDataService.PublicDataSyncFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataSyncScheduler {

    private final PublicDataSyncFacadeService publicDataSyncFacadeService;

    /**
     * 매일 새벽 03:00에 실행
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runAutoSync() {
        log.info("[스케줄러] 공공데이터 전체 자동 비동기 동기화 배치 시작");
        try {
            publicDataSyncFacadeService.syncAllAsync();
            log.info("[스케줄러] 공공데이터 동기화 스레드 배치 트리거 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 동기화 실행 중 예외 발생", e);
        }
    }
}