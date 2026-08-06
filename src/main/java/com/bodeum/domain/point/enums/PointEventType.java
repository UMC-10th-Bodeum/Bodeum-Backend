package com.bodeum.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointEventType {
    COMMUNITY_POST_CREATED(PointType.POST_CREATED),
    COMMUNITY_ANSWER_CREATED(PointType.ANSWER_CREATED),
    COMMUNITY_POST_LIKE_RECEIVED(PointType.LIKE_RECEIVED),
    COMMUNITY_ANSWER_LIKE_RECEIVED(PointType.LIKE_RECEIVED),
    COMMUNITY_ANSWER_ACCEPTED(PointType.ANSWER_ACCEPTED);

    private final PointType pointType;
}
