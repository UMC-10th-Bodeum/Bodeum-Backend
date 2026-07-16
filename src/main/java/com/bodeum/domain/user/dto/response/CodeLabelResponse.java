package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.user.enumtype.DisabilityType;
import com.bodeum.domain.user.enumtype.InterestCategory;

public record CodeLabelResponse(
        String code,
        String label
) {

    public static CodeLabelResponse from(DisabilityType type) {
        return new CodeLabelResponse(type.name(), type.getLabel());
    }

    public static CodeLabelResponse from(InterestCategory category) {
        return new CodeLabelResponse(category.name(), category.getLabel());
    }
}
