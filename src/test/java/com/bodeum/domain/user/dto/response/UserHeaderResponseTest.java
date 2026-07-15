package com.bodeum.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enumtype.DisabilityType;
import java.util.Collections;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserHeaderResponseTest {

    @Test
    void loggedOutExposesOnlyIsLoggedInFalse() {
        UserHeaderResponse response = UserHeaderResponse.loggedOut();

        assertThat(response.isLoggedIn()).isFalse();
        assertThat(response.nickname()).isNull();
        assertThat(response.level()).isNull();
        assertThat(response.badgeName()).isNull();
        assertThat(response.childDisabilityTypes()).isNull();
        assertThat(response.childAge()).isNull();
        assertThat(response.region()).isNull();
    }

    @Test
    void newUserStartsAtLevelOneSprout() {
        // 신규 가입자는 포인트 0 → Level 1 새싹
        UserHeaderResponse response = UserHeaderResponse.from(newUser());

        assertThat(response.isLoggedIn()).isTrue();
        assertThat(response.nickname()).isEqualTo("민준맘");
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.badgeName()).isEqualTo("새싹");
    }

    @Test
    void exposesRegionFullName() {
        User user = newUser();
        user.updateInterestRegion(Collections.emptyList(), Region.create("서울특별시", "강남구"));

        assertThat(UserHeaderResponse.from(user).region()).isEqualTo("서울특별시 강남구");
    }

    @Test
    void regionIsNullWhenNotRegistered() {
        assertThat(UserHeaderResponse.from(newUser()).region()).isNull();
    }

    @Test
    void calculatesChildAgeWhenBirthMonthAlreadyPassed() {
        int birthYear = LocalDate.now().getYear() - 5;
        User user = newUser();
        // 1월생은 어느 시점에 조회해도 당해 생월이 지났거나 당월이므로 만 나이 = 연도 차이
        user.updateChildProfile(
                "민준이",
                String.format("%04d-01", birthYear),
                List.of(DisabilityType.AUTISM),
                null
        );

        UserHeaderResponse response = UserHeaderResponse.from(user);

        assertThat(response.childAge()).isEqualTo(5);
        assertThat(response.childDisabilityTypes()).containsExactly(DisabilityType.AUTISM);
    }

    @Test
    void subtractsOneYearWhenBirthMonthNotYetReached() {
        LocalDate now = LocalDate.now();
        int birthYear = now.getYear() - 3;
        User user = newUser();
        user.updateChildProfile(
                "민준이",
                String.format("%04d-12", birthYear),
                List.of(DisabilityType.ADHD),
                null
        );

        // 12월생: 아직 12월 전이면 생일 전이므로 한 살 적게 계산된다.
        int expected = now.getMonthValue() < 12 ? 2 : 3;
        assertThat(UserHeaderResponse.from(user).childAge()).isEqualTo(expected);
    }

    @Test
    void childAgeIsNullWhenBirthYearMissing() {
        assertThat(UserHeaderResponse.from(newUser()).childAge()).isNull();
    }

    private User newUser() {
        return User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
    }
}
