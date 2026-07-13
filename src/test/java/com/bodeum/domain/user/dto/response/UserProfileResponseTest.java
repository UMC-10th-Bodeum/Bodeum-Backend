package com.bodeum.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
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
        user.updateChildProfile("민준", "2020-03", List.of(1, 3), "언어치료");

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.joinedAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.childProfile().nickname()).isEqualTo("민준");
        assertThat(response.childProfile().birth()).isEqualTo("2020-03");
        assertThat(response.childProfile().disabilityTypeIds()).containsExactly(1, 3);
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
        user.updateInterestRegion(List.of(1), region);

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.regionLevel1()).isEqualTo("서울특별시");
        assertThat(response.regionLevel2()).isEqualTo("강남구");
    }
}
