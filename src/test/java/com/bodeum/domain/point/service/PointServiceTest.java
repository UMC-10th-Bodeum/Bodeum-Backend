package com.bodeum.domain.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.entity.GuardianPoint;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository.PointActivitySummary;
import com.bodeum.domain.point.repository.GuardianPointRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private GuardianPointRepository guardianPointRepository;

    @Mock
    private GuardianPointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

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
}
