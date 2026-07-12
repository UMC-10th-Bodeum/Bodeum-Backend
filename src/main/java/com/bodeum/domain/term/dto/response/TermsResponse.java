package com.bodeum.domain.term.dto.response;

import com.bodeum.domain.term.enumtype.TermType;
import java.time.LocalDateTime;

public record TermsResponse(
        String type,
        String title,
        String content,
        LocalDateTime updatedAt
) {

    public static TermsResponse of(
            TermType type,
            String content,
            LocalDateTime updatedAt
    ) {
        return new TermsResponse(
                type.getPath(),
                type.getTitle(),
                content,
                updatedAt
        );
    }
}
