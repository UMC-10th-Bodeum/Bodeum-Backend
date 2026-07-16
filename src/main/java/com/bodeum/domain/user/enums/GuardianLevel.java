package com.bodeum.domain.user.enums;

import java.util.Arrays;

public enum GuardianLevel {

    SPROUT(1, "새싹", 0, 49, "보듬에 첫발을 내딛고 정보를 탐색하기 시작한 새싹 단계입니다."),
    LEAF(2, "잎새", 50, 199, "이웃 보호자들과 소소한 일상과 유익한 정보를 주고받으며 푸르게 자라나는 단계입니다."),
    FLOWER(3, "꽃", 200, 499, "정성스러운 답변과 경험 공유로 커뮤니티에 본격적인 신뢰의 결실을 피워내는 단계입니다."),
    FRUIT(4, "열매", 500, 999, "풍부한 내공이 담긴 답변으로 이웃 유저들에게 실질적인 해결책과 지혜를 나누는 단계입니다."),
    TREE(5, "나무", 1000, Integer.MAX_VALUE, "흔들리지 않는 깊은 지혜로 이웃 부모들의 든든한 버팀목이 되어주는 선배 보호자 단계입니다.");

    private final int levelNumber;
    private final String badgeName;
    private final int minPoint;
    private final int maxPoint;
    private final String description;

    GuardianLevel(int levelNumber, String badgeName, int minPoint, int maxPoint, String description) {
        this.levelNumber = levelNumber;
        this.badgeName = badgeName;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.description = description;
    }

    public static GuardianLevel from(int point) {
        return Arrays.stream(values())
                .filter(level -> point >= level.minPoint && point <= level.maxPoint)
                .findFirst()
                .orElse(SPROUT);
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public String getDescription() {
        return description;
    }
}
