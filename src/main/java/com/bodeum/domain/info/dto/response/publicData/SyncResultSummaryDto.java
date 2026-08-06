package com.bodeum.domain.info.dto.response.publicData;

import java.time.LocalDateTime;
import java.util.List;

// 전체 동기화 요약 DTO

public record SyncResultSummaryDto(
        LocalDateTime executedAt,
        int totalExecutedCount,
        int successCount,
        int failCount,
        List<SyncDetailResultDto> details
) {
    public static SyncResultSummaryDto of(List<SyncDetailResultDto> details) {
        int total = details.size();
        int success = (int) details.stream().filter(SyncDetailResultDto::isSuccess).count();
        int fail = total - success;

        return new SyncResultSummaryDto(
                LocalDateTime.now(),
                total,
                success,
                fail,
                details
        );
    }
}