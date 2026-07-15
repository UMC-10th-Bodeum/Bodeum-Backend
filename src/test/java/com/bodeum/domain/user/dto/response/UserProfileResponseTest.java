package com.bodeum.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enumtype.DisabilityType;
import com.bodeum.domain.user.enumtype.InterestCategory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserProfileResponseTest {

    @Test
    void exposesJoinedAtAndNestedChildProfile() {
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
        // createdAt/updatedAt은 JPA Auditing이 영속화 시점에 채우므로, 단위 테스트에서는 영속 상태를 모사한다.
        Instant persistedAt = Instant.parse("2026-07-16T00:00:00Z");
        ReflectionTestUtils.setField(user, "createdAt", persistedAt);
        ReflectionTestUtils.setField(user, "updatedAt", persistedAt);

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.joinedAt()).isEqualTo(persistedAt);
        assertThat(response.updatedAt()).isEqualTo(persistedAt);
        assertThat(response.childProfile().nickname()).isEqualTo("민준");
        assertThat(response.childProfile().birth()).isEqualTo("2020-03");
        assertThat(response.childProfile().disabilityTypes()).containsExactly(
                DisabilityType.AUTISM,
                DisabilityType.CEREBRAL_PALSY
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
        user.updateInterestRegion(List.of(InterestCategory.INSTITUTION), region);

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.regionLevel1()).isEqualTo("서울특별시");
        assertThat(response.regionLevel2()).isEqualTo("강남구");
        assertThat(response.interestCategories()).containsExactly(InterestCategory.INSTITUTION);
    }
}
