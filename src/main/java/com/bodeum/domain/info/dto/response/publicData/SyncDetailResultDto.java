package com.bodeum.domain.info.dto.response.publicData;

import com.bodeum.domain.info.entity.enums.MainCategory;

// 개별 동기화 결과 DTO

public record SyncDetailResultDto(
        Long categoryId,
        MainCategory mainCategory,
        String serviceName,
        boolean isSuccess,
        int insertedCount,
        int updatedCount,
        String errorMessage
) {
    public static SyncDetailResultDto success(Long categoryId, MainCategory mainCategory, String serviceName, int inserted, int updated) {
        return new SyncDetailResultDto(categoryId, mainCategory, serviceName, true, inserted, updated, null);
    }

    public static SyncDetailResultDto fail(Long categoryId, MainCategory mainCategory, String serviceName, String errorMessage) {
        return new SyncDetailResultDto(categoryId, mainCategory, serviceName, false, 0, 0, errorMessage);
    }
}