package com.bodeum.domain.onboarding.dto.response;

import com.bodeum.domain.user.dto.response.CodeLabelResponse;
import com.bodeum.domain.user.enumtype.DisabilityType;
import com.bodeum.domain.user.enumtype.InterestCategory;
import java.util.Arrays;
import java.util.List;

public record ProfileSelectionOptionsResponse(
        List<CodeLabelResponse> disabilityTypes,
        List<CodeLabelResponse> interestCategories
) {

    public static ProfileSelectionOptionsResponse fromEnums() {
        return new ProfileSelectionOptionsResponse(
                Arrays.stream(DisabilityType.values())
                        .map(CodeLabelResponse::from)
                        .toList(),
                Arrays.stream(InterestCategory.values())
                        .map(CodeLabelResponse::from)
                        .toList()
        );
    }
}
