package com.bodeum.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.global.config.JpaAuditingConfig;
import com.bodeum.global.config.QueryDslConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * 애그리거트 자식(child_profiles·user_interests·guardian_profiles·user_agreements)만
 * 바뀌어도 users.updated_at이 갱신되는지 H2로 검증한다.
 * @LastModifiedDate는 부모 행이 dirty일 때만 동작하므로 단위 테스트로는 잡히지 않는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, JpaAuditingConfig.class})
class UserAggregateAuditingIntegrationTest {

    private static final Instant BACKDATED = Instant.parse("2020-01-01T00:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("자녀 프로필만 수정해도 users.updated_at이 갱신된다")
    void childProfileUpdateRefreshesUserUpdatedAt() {
        Long userId = persistBackdatedUser("kakao-child");

        User user = em.find(User.class, userId);
        user.updateChildProfile("민준", "2020-03", List.of(DisabilityType.AUTISM), "언어치료");
        em.flush();
        em.clear();

        assertThat(em.find(User.class, userId).getUpdatedAt()).isAfter(BACKDATED);
    }

    @Test
    @DisplayName("관심사·지역만 수정해도 users.updated_at이 갱신된다")
    void interestRegionUpdateRefreshesUserUpdatedAt() {
        Long userId = persistBackdatedUser("kakao-interest");

        User user = em.find(User.class, userId);
        user.updateInterestRegion(List.of(InterestCategory.WELFARE_SUBSIDY), null);
        em.flush();
        em.clear();

        assertThat(em.find(User.class, userId).getUpdatedAt()).isAfter(BACKDATED);
    }

    @Test
    @DisplayName("약관 동의만 등록해도 users.updated_at이 갱신된다")
    void agreementRefreshesUserUpdatedAt() {
        Long userId = persistBackdatedUser("kakao-agreement");

        User user = em.find(User.class, userId);
        user.agreeTerms(true, true, true);
        em.flush();
        em.clear();

        assertThat(em.find(User.class, userId).getUpdatedAt()).isAfter(BACKDATED);
    }

    @Test
    @DisplayName("닉네임 변경 없이 자녀 정보만 담긴 프로필 수정도 users.updated_at을 갱신한다")
    void profileUpdateWithoutNicknameRefreshesUserUpdatedAt() {
        Long userId = persistBackdatedUser("kakao-profile");

        User user = em.find(User.class, userId);
        user.updateProfile(
                null,
                "민준",
                "2020-03",
                List.of(DisabilityType.DEVELOPMENTAL_DELAY),
                "사회성 발달",
                null,
                null,
                null,
                null
        );
        em.flush();
        em.clear();

        assertThat(em.find(User.class, userId).getUpdatedAt()).isAfter(BACKDATED);
    }

    /**
     * 감사 리스너가 넣은 현재 시각과 갱신 후 시각이 시계 해상도 안에서 같아질 수 있으므로,
     * updated_at을 과거로 내려둔 뒤 갱신 여부를 판정한다.
     */
    private Long persistBackdatedUser(String providerUserId) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, providerUserId, providerUserId + "@example.com", "닉네임");
        em.persist(user);
        em.flush();

        Long userId = user.getId();
        em.getEntityManager()
                .createQuery("update User u set u.updatedAt = :backdated where u.id = :id")
                .setParameter("backdated", BACKDATED)
                .setParameter("id", userId)
                .executeUpdate();
        em.clear();

        assertThat(em.find(User.class, userId).getUpdatedAt()).isEqualTo(BACKDATED);
        em.clear();
        return userId;
    }
}
