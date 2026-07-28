package com.bodeum.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.domain.user.enums.InterestCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserProfileResponseTest {

    @Test
    void exposesNestedChildProfile() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
        user.updateChildProfile(
                "민준",
                "2020-03",
                List.of(DisabilityType.AUTISM, DisabilityType.CEREBRAL_PALSY),
                "언어치료"
        );

        UserProfileResponse response = UserProfileResponse.from(user, 0);

        assertThat(response.childProfile().nickname()).isEqualTo("민준");
        assertThat(response.childProfile().birth()).isEqualTo("2020-03");
        assertThat(response.childProfile().disabilityTypes()).containsExactly(
                new CodeLabelResponse("AUTISM", "자폐스펙트럼"),
                new CodeLabelResponse("CEREBRAL_PALSY", "뇌병변장애")
        );
    }

    @Test
    void exposesRegionIdWithRegionNames() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
        Region region = Region.create("서울특별시", "강남구");
        ReflectionTestUtils.setField(region, "id", 10L);
        user.updateInterestRegion(List.of(InterestCategory.WELFARE_SUBSIDY), region);

        UserProfileResponse response = UserProfileResponse.from(user, 0);

        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.regionLevel1()).isEqualTo("서울특별시");
        assertThat(response.regionLevel2()).isEqualTo("강남구");
        assertThat(response.interestCategories()).containsExactly(new CodeLabelResponse("WELFARE_SUBSIDY", "맞춤 복지 지원금"));
    }

    @Test
    void mapsInjectedTotalPointToLevelFields() {
        // 총점/레벨/뱃지/레벨설명은 주입된 총점 기준으로 계산된다 (230 → Level 3 꽃)
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );

        UserProfileResponse response = UserProfileResponse.from(user, 230);

        assertThat(response.point()).isEqualTo(230);
        assertThat(response.level()).isEqualTo(3);
        assertThat(response.badgeName()).isEqualTo("꽃");
        assertThat(response.levelDescription())
                .isEqualTo("정성스러운 답변과 경험 공유로 커뮤니티에 본격적인 신뢰의 결실을 피워내는 단계입니다.");
    }
}
