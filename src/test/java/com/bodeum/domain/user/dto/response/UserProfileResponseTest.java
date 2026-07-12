package com.bodeum.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.entity.UserAccount;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserProfileResponseTest {

    @Test
    void exposesJoinedAtAndNestedChildProfile() {
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
        userAccount.updateChildProfile("민준", "2020-03", List.of(1, 3), "언어치료");

        UserProfileResponse response = UserProfileResponse.from(userAccount);

        assertThat(response.joinedAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.childProfile().nickname()).isEqualTo("민준");
        assertThat(response.childProfile().birth()).isEqualTo("2020-03");
        assertThat(response.childProfile().disabilityTypeIds()).containsExactly(1, 3);
    }
}
