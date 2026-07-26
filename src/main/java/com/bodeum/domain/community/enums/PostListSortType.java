package com.bodeum.domain.community.enums;

import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import java.util.Locale;
import org.springframework.data.domain.Sort;

public enum PostListSortType {

    VIEW("viewCount"),
    SCRAP("scrapCount"),
    COMMENT("commentCount");

    private final String property;

    PostListSortType(String property) {
        this.property = property;
    }

    public static PostListSortType from(String value) {
        if (value == null || value.isBlank()) {
            return VIEW;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CommunityException(CommunityErrorCode.INVALID_POST_LIST_SORT);
        }
    }

    public Sort toSort() {
        return Sort.by(
                Sort.Order.desc(property),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
    }
}
