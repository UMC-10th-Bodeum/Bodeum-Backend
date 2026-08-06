package com.bodeum.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterestCategory {
    WELFARE_SUBSIDY("맞춤 복지 지원금"),
    HOSPITAL_HEALTH("안심 병원, 건강"),
    PARENTING_COMMUNICATION("육아 상담, 소통"),
    GROWTH_EDUCATION("성장, 교육");

    private final String label;
}
