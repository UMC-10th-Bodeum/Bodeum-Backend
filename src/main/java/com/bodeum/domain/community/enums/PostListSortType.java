package com.bodeum.domain.community.enums;

import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import java.util.Locale;
import org.springframework.data.domain.Sort;

public enum PostListSortType {

    LATEST("createdAt"),
    VIEW("viewCount"),
    LIKE("likeCount"),
    COMMENT("commentCount");

    public static final String LOGGED_IN_DEFAULT_SORT_VALUE = "latest";

    private final String property;

    PostListSortType(String property) {
        this.property = property;
    }

    public static PostListSortType from(String value) {
        return from(value, VIEW);
    }

    public static PostListSortType from(String value, PostListSortType defaultSortType) {
        if (value == null || value.isBlank()) {
            return defaultSortType;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CommunityException(CommunityErrorCode.INVALID_POST_LIST_SORT);
        }
    }

    public Sort toSort() {
        if (this == LATEST) {
            return Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        }

        return Sort.by(
                Sort.Order.desc(property),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
    }
}
