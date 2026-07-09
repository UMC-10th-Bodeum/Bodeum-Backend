package com.bodeum.domain.term.dto.response;

import com.bodeum.domain.term.enumtype.TermType;
import java.time.LocalDate;

public record TermsResponse(
        String type,
        String title,
        String version,
        LocalDate effectiveDate,
        String content,
        boolean required
) {

    public static TermsResponse of(
            TermType type,
            String version,
            LocalDate effectiveDate,
            String content,
            boolean isRequired
    ) {
        return new TermsResponse(
                type.getPath(),
                type.getTitle(),
                version,
                effectiveDate,
                content,
                isRequired
        );
    }
}
