package com.bodeum.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisabilityType {
    AUTISM("자폐스펙트럼"),
    INTELLECTUAL_DISABILITY("지적장애"),
    CEREBRAL_PALSY("뇌병변장애"),
    ADHD("ADHD"),
    DEVELOPMENTAL_DELAY("발달지연"),
    LANGUAGE_DISORDER("언어장애"),
    ETC("기타");

    private final String label;
}
