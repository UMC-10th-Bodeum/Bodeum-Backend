package com.bodeum.domain.onboarding.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.domain.user.enums.InterestCategory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileSelectionValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createChildProfileRejectsDuplicateDisabilityTypes() {
        CreateChildProfileRequest request = new CreateChildProfileRequest(
                "민준이",
                "2020-01",
                List.of(DisabilityType.AUTISM, DisabilityType.AUTISM),
                null
        );

        assertThat(messagesOf(request))
                .contains("집중 케어 영역은 중복 선택할 수 없습니다.");
    }

    @Test
    void createInterestRegionRejectsDuplicateInterestCategories() {
        CreateInterestRegionRequest request = new CreateInterestRegionRequest(
                List.of(InterestCategory.WELFARE_SUBSIDY, InterestCategory.WELFARE_SUBSIDY),
                1L
        );

        assertThat(messagesOf(request))
                .contains("관심사는 중복 선택할 수 없습니다.");
    }

    @Test
    void updateProfileRejectsDuplicateSelections() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                List.of(DisabilityType.AUTISM, DisabilityType.AUTISM),
                null,
                List.of(InterestCategory.WELFARE_SUBSIDY, InterestCategory.WELFARE_SUBSIDY),
                null,
                null,
                null
        );

        assertThat(messagesOf(request))
                .contains(
                        "집중 케어 영역은 중복 선택할 수 없습니다.",
                        "관심사는 중복 선택할 수 없습니다."
                );
    }

    private List<String> messagesOf(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .toList();
    }
}
