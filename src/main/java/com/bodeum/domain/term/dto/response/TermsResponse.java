package com.bodeum.domain.term.dto.response;

import com.bodeum.domain.term.enumtype.TermType;
import java.time.Instant;

public record TermsResponse(
        String type,
        String title,
        String content,
        Instant updatedAt
) {

    public static TermsResponse of(
            TermType type,
            String content,
            Instant updatedAt
    ) {
        return new TermsResponse(
                type.getPath(),
                type.getTitle(),
                content,
                updatedAt
        );
    }
}
