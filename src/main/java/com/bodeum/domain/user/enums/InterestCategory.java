package com.bodeum.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterestCategory {
    INSTITUTION("기관"),
    HOSPITAL("병원"),
    WELFARE("복지"),
    EMPLOYMENT("취업"),
    EDUCATION("교육");

    private final String label;
}
