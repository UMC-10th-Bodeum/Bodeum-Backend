package com.bodeum.domain.point.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository;
import com.bodeum.domain.point.repository.GuardianPointRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.global.config.JpaAuditingConfig;
import com.bodeum.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, JpaAuditingConfig.class, PointService.class})
class PointRewardPersistenceIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GuardianPointRepository guardianPointRepository;

    @Autowired
    private GuardianPointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointService pointService;

    @Test
    void grantsExactActivityPointsPreventsDuplicatesAndRevokesCanceledEvent() {
        User recipient = User.createSocialUser(
                SocialProvider.KAKAO,
                "point-recipient",
                "recipient@example.com",
                "포인트 사용자"
        );
        entityManager.persistAndFlush(recipient);
        Long recipientUserId = recipient.getId();

        assertThat(pointService.grantActivityPoint(
                recipientUserId,
                PointEventType.COMMUNITY_POST_CREATED,
                100L,
                recipientUserId
        )).isTrue();
        assertThat(pointService.grantActivityPoint(
                recipientUserId,
                PointEventType.COMMUNITY_ANSWER_CREATED,
                200L,
                recipientUserId
        )).isTrue();
        assertThat(pointService.grantActivityPoint(
                recipientUserId,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                100L,
                2L
        )).isTrue();
        assertThat(pointService.grantActivityPoint(
                recipientUserId,
                PointEventType.COMMUNITY_ANSWER_ACCEPTED,
                200L,
                3L
        )).isTrue();

        assertThat(pointService.grantActivityPoint(
                recipientUserId,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                100L,
                2L
        )).isFalse();

        entityManager.flush();
        entityManager.clear();

        MyPointResponse earned = pointService.getMyPoints(recipientUserId);
        assertThat(earned.totalPoint()).isEqualTo(34);
        assertActivity(earned, PointType.POST_CREATED, 5L, 1L);
        assertActivity(earned, PointType.ANSWER_CREATED, 4L, 1L);
        assertActivity(earned, PointType.LIKE_RECEIVED, 5L, 1L);
        assertActivity(earned, PointType.ANSWER_ACCEPTED, 20L, 1L);

        assertThat(pointService.revokeActivityPoint(
                recipientUserId,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                100L,
                2L
        )).isTrue();

        entityManager.flush();
        entityManager.clear();

        MyPointResponse revoked = pointService.getMyPoints(recipientUserId);
        assertThat(revoked.totalPoint()).isEqualTo(29);
        assertActivity(revoked, PointType.LIKE_RECEIVED, 0L, 0L);
        assertThat(guardianPointRepository.findByUserId(recipientUserId)).isPresent();
        assertThat(pointHistoryRepository.findAll()).hasSize(3);
    }

    private void assertActivity(
            MyPointResponse response,
            PointType pointType,
            long earnedPoint,
            long activityCount
    ) {
        assertThat(response.activities())
                .filteredOn(activity -> activity.pointType() == pointType)
                .singleElement()
                .satisfies(activity -> {
                    assertThat(activity.earnedPoint()).isEqualTo(earnedPoint);
                    assertThat(activity.activityCount()).isEqualTo(activityCount);
                });
    }
}
