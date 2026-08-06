package com.bodeum.domain.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.entity.GuardianPoint;
import com.bodeum.domain.point.entity.GuardianPointHistory;
import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository.PointActivitySummary;
import com.bodeum.domain.point.repository.GuardianPointRepository;
import com.bodeum.domain.point.repository.GuardianPointRepository.UserTotalPoint;
import com.bodeum.domain.user.entity.GuardianProfile;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private GuardianPointRepository guardianPointRepository;

    @Mock
    private GuardianPointHistoryRepository pointHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    void grantActivityPointIncreasesTotalAndStoresEventHistory() {
        User user = activeUser(1L);
        GuardianProfile guardianProfile = user.ensureGuardianProfile();
        ReflectionTestUtils.setField(guardianProfile, "id", 11L);
        GuardianPoint guardianPoint = guardianPoint(7L, 11L, 0);

        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
        given(guardianPointRepository.findByGuardianProfileId(11L))
                .willReturn(Optional.of(guardianPoint));
        given(pointHistoryRepository
                .existsByGuardianPoint_IdAndEventTypeAndReferenceIdAndActorUserId(
                        7L,
                        PointEventType.COMMUNITY_POST_CREATED,
                        100L,
                        1L
                )).willReturn(false);

        boolean granted = pointService.grantActivityPoint(
                1L,
                PointEventType.COMMUNITY_POST_CREATED,
                100L,
                1L
        );

        assertThat(granted).isTrue();
        assertThat(guardianPoint.getTotalPoint()).isEqualTo(5);
        var historyCaptor = org.mockito.ArgumentCaptor.forClass(GuardianPointHistory.class);
        then(pointHistoryRepository).should().save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getPointType()).isEqualTo(PointType.POST_CREATED);
        assertThat(historyCaptor.getValue().getPointValue()).isEqualTo(5);
        assertThat(historyCaptor.getValue().getEventType())
                .isEqualTo(PointEventType.COMMUNITY_POST_CREATED);
        assertThat(historyCaptor.getValue().getReferenceId()).isEqualTo(100L);
        assertThat(historyCaptor.getValue().getActorUserId()).isEqualTo(1L);
    }

    @Test
    void grantActivityPointSkipsDuplicateEvent() {
        User user = activeUser(1L);
        GuardianProfile guardianProfile = user.ensureGuardianProfile();
        ReflectionTestUtils.setField(guardianProfile, "id", 11L);
        GuardianPoint guardianPoint = guardianPoint(7L, 11L, 5);

        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
        given(guardianPointRepository.findByGuardianProfileId(11L))
                .willReturn(Optional.of(guardianPoint));
        given(pointHistoryRepository
                .existsByGuardianPoint_IdAndEventTypeAndReferenceIdAndActorUserId(
                        7L,
                        PointEventType.COMMUNITY_POST_CREATED,
                        100L,
                        1L
                )).willReturn(true);

        boolean granted = pointService.grantActivityPoint(
                1L,
                PointEventType.COMMUNITY_POST_CREATED,
                100L,
                1L
        );

        assertThat(granted).isFalse();
        assertThat(guardianPoint.getTotalPoint()).isEqualTo(5);
        then(pointHistoryRepository).should(never()).save(any(GuardianPointHistory.class));
    }

    @Test
    void revokeActivityPointDecreasesTotalAndDeletesEventHistory() {
        User user = activeUser(1L);
        GuardianPoint guardianPoint = guardianPoint(7L, 11L, 5);
        GuardianPointHistory history = GuardianPointHistory.create(
                guardianPoint,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                100L,
                2L
        );

        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
        given(guardianPointRepository.findByUserId(1L))
                .willReturn(Optional.of(guardianPoint));
        given(pointHistoryRepository
                .findByGuardianPoint_IdAndEventTypeAndReferenceIdAndActorUserId(
                        7L,
                        PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                        100L,
                        2L
                )).willReturn(Optional.of(history));

        boolean revoked = pointService.revokeActivityPoint(
                1L,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                100L,
                2L
        );

        assertThat(revoked).isTrue();
        assertThat(guardianPoint.getTotalPoint()).isZero();
        then(pointHistoryRepository).should().delete(history);
    }

    @Test
    void getMyPointsReturnsGuardianPointTotalAndAggregatedHistory() {
        GuardianPoint guardianPoint = org.mockito.Mockito.mock(GuardianPoint.class);
        PointActivitySummary postSummary =
                org.mockito.Mockito.mock(PointActivitySummary.class);
        PointActivitySummary acceptedSummary =
                org.mockito.Mockito.mock(PointActivitySummary.class);

        given(guardianPointRepository.findByUserId(1L))
                .willReturn(Optional.of(guardianPoint));
        given(guardianPoint.getId()).willReturn(7L);
        given(guardianPoint.getTotalPoint()).willReturn(42);
        given(pointHistoryRepository.summarizeByGuardianPointId(7L))
                .willReturn(List.of(postSummary, acceptedSummary));

        given(postSummary.getPointType()).willReturn(PointType.POST_CREATED);
        given(postSummary.getEarnedPoint()).willReturn(10L);
        given(postSummary.getActivityCount()).willReturn(2L);
        given(acceptedSummary.getPointType()).willReturn(PointType.ANSWER_ACCEPTED);
        given(acceptedSummary.getEarnedPoint()).willReturn(20L);
        given(acceptedSummary.getActivityCount()).willReturn(1L);

        MyPointResponse response = pointService.getMyPoints(1L);

        assertThat(response.totalPoint()).isEqualTo(42);
        assertThat(response.activities())
                .extracting(MyPointResponse.PointActivity::pointType)
                .containsExactly(PointType.values());
        assertThat(response.activities().get(0).earnedPoint()).isEqualTo(10L);
        assertThat(response.activities().get(0).activityCount()).isEqualTo(2L);
        assertThat(response.activities().get(1).earnedPoint()).isZero();
        assertThat(response.activities().get(2).earnedPoint()).isZero();
        assertThat(response.activities().get(3).earnedPoint()).isEqualTo(20L);
        assertThat(response.activities().get(3).activityCount()).isEqualTo(1L);
    }

    @Test
    void getMyPointsReturnsZeroActivitiesWhenPointAggregateDoesNotExist() {
        given(guardianPointRepository.findByUserId(1L))
                .willReturn(Optional.empty());

        MyPointResponse response = pointService.getMyPoints(1L);

        assertThat(response.totalPoint()).isZero();
        assertThat(response.activities()).hasSize(PointType.values().length);
        assertThat(response.activities())
                .allSatisfy(activity -> {
                    assertThat(activity.earnedPoint()).isZero();
                    assertThat(activity.activityCount()).isZero();
                });
        verifyNoInteractions(pointHistoryRepository);
    }

    @Test
    void getTotalPointReturnsGuardianPointTotal() {
        GuardianPoint guardianPoint = org.mockito.Mockito.mock(GuardianPoint.class);
        given(guardianPointRepository.findByUserId(1L))
                .willReturn(Optional.of(guardianPoint));
        given(guardianPoint.getTotalPoint()).willReturn(42);

        assertThat(pointService.getTotalPoint(1L)).isEqualTo(42);
    }

    @Test
    void getTotalPointReturnsZeroWhenPointAggregateDoesNotExist() {
        given(guardianPointRepository.findByUserId(1L))
                .willReturn(Optional.empty());

        assertThat(pointService.getTotalPoint(1L)).isZero();
    }

    @Test
    void getTotalPointsReturnsTotalsByUserId() {
        UserTotalPoint first = org.mockito.Mockito.mock(UserTotalPoint.class);
        UserTotalPoint second = org.mockito.Mockito.mock(UserTotalPoint.class);
        Set<Long> userIds = Set.of(1L, 2L);

        given(guardianPointRepository.findTotalPointsByUserIdIn(userIds))
                .willReturn(List.of(first, second));
        given(first.getUserId()).willReturn(1L);
        given(first.getTotalPoint()).willReturn(42);
        given(second.getUserId()).willReturn(2L);
        given(second.getTotalPoint()).willReturn(200);

        assertThat(pointService.getTotalPoints(userIds))
                .isEqualTo(Map.of(1L, 42, 2L, 200));
    }

    @Test
    void getTotalPointsSkipsRepositoryWhenUserIdsAreEmpty() {
        assertThat(pointService.getTotalPoints(Set.of())).isEmpty();

        verifyNoInteractions(guardianPointRepository);
    }

    private User activeUser(Long userId) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "provider-" + userId,
                "user@example.com",
                "사용자"
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private GuardianPoint guardianPoint(Long pointId, Long guardianProfileId, int totalPoint) {
        GuardianPoint guardianPoint = GuardianPoint.create(guardianProfileId);
        ReflectionTestUtils.setField(guardianPoint, "id", pointId);
        guardianPoint.increasePoint(totalPoint);
        return guardianPoint;
    }
}
