package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.domain.user.enums.InterestCategory;

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
