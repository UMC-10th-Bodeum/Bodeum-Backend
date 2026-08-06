package com.bodeum.domain.point.enums;

public enum PointType {
    POST_CREATED("게시글 작성", 5),
    ANSWER_CREATED("답변 작성", 4),
    LIKE_RECEIVED("도움돼요", 5),
    ANSWER_ACCEPTED("답변 채택", 20);

    private final String label;
    private final int pointPerAction;

    PointType(String label, int pointPerAction) {
        this.label = label;
        this.pointPerAction = pointPerAction;
    }

    public String getLabel() {
        return label;
    }

    public int getPointPerAction() {
        return pointPerAction;
    }
}
