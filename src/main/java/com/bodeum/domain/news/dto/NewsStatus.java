package com.bodeum.domain.news.dto;

import com.bodeum.domain.news.entity.RecruitmentStatus;

public enum NewsStatus {
    RECRUITING(RecruitmentStatus.OPEN),
    CLOSED(RecruitmentStatus.CLOSED),
    ALWAYS_OPEN(RecruitmentStatus.ALWAYS_OPEN),
    UPCOMING(RecruitmentStatus.UPCOMING);

    private final RecruitmentStatus recruitmentStatus;

    NewsStatus(RecruitmentStatus recruitmentStatus) {
        this.recruitmentStatus = recruitmentStatus;
    }

    public RecruitmentStatus toEntity() {
        return recruitmentStatus;
    }

    public static NewsStatus from(RecruitmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN -> RECRUITING;
            case CLOSED -> CLOSED;
            case ALWAYS_OPEN -> ALWAYS_OPEN;
            case UPCOMING -> UPCOMING;
        };
    }
}
