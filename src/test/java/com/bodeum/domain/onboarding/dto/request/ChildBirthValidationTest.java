package com.bodeum.domain.onboarding.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.enums.DisabilityType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChildBirthValidationTest {

    private static final String BIRTH_RANGE_MESSAGE =
            "생년월은 1990-01부터 현재 연월까지의 YYYY-MM 형식으로 입력해주세요.";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"1990-01", "1999-12", "2020-03"})
    void createChildProfileAcceptsBirthFrom1990(String birth) {
        assertThat(messagesOf(createChildProfile(birth)))
                .doesNotContain(BIRTH_RANGE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1989-12", "1900-01"})
    void createChildProfileRejectsBirthBefore1990(String birth) {
        assertThat(messagesOf(createChildProfile(birth)))
                .contains(BIRTH_RANGE_MESSAGE);
    }

    @Test
    void createChildProfileAcceptsCurrentYearMonth() {
        String currentBirth = YearMonth.now().toString();

        assertThat(messagesOf(createChildProfile(currentBirth)))
                .doesNotContain(BIRTH_RANGE_MESSAGE);
    }

    @Test
    void createChildProfileRejectsFutureBirth() {
        String futureBirth = YearMonth.now().plusMonths(1).toString();

        assertThat(messagesOf(createChildProfile(futureBirth)))
                .contains(BIRTH_RANGE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1990-01", "1999-12", "2020-03"})
    void updateProfileAcceptsBirthFrom1990(String birth) {
        assertThat(messagesOf(updateProfile(birth)))
                .doesNotContain(BIRTH_RANGE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1989-12", "1900-01"})
    void updateProfileRejectsBirthBefore1990(String birth) {
        assertThat(messagesOf(updateProfile(birth)))
                .contains(BIRTH_RANGE_MESSAGE);
    }

    @Test
    void updateProfileAllowsNullBirth() {
        assertThat(messagesOf(updateProfile(null)))
                .doesNotContain(BIRTH_RANGE_MESSAGE);
    }

    private CreateChildProfileRequest createChildProfile(String birth) {
        return new CreateChildProfileRequest(
                "민준이",
                birth,
                List.of(DisabilityType.AUTISM),
                null
        );
    }

    private UpdateUserProfileRequest updateProfile(String birth) {
        return new UpdateUserProfileRequest(
                null,
                null,
                birth,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private List<String> messagesOf(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .toList();
    }
}
