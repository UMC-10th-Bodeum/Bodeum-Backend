package com.bodeum.domain.point.service;

import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.dto.response.MyPointResponse.PointActivity;
import com.bodeum.domain.point.entity.GuardianPoint;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository;
import com.bodeum.domain.point.repository.GuardianPointHistoryRepository.PointActivitySummary;
import com.bodeum.domain.point.repository.GuardianPointRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final GuardianPointRepository guardianPointRepository;
    private final GuardianPointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public MyPointResponse getMyPoints(Long userId) {
        Optional<GuardianPoint> guardianPoint =
                guardianPointRepository.findByUserId(userId);

        Map<PointType, PointActivitySummary> summaries =
                guardianPoint
                        .map(this::getActivitySummaries)
                        .orElseGet(() -> new EnumMap<>(PointType.class));

        List<PointActivity> activities = Arrays.stream(PointType.values())
                .map(pointType -> toActivity(pointType, summaries.get(pointType)))
                .toList();

        int totalPoint = guardianPoint
                .map(GuardianPoint::getTotalPoint)
                .orElse(0);

        return new MyPointResponse(totalPoint, activities);
    }

    @Transactional(readOnly = true)
    public int getTotalPoint(Long userId) {
        return guardianPointRepository.findByUserId(userId)
                .map(GuardianPoint::getTotalPoint)
                .orElse(0);
    }

    /**
     * 회원 탈퇴 시 해당 회원의 포인트 데이터를 삭제한다.
     *
     * <p>GuardianPoint는 guardianProfileId를 단순 컬럼으로만 들고 있어 GuardianProfile의
     * orphanRemoval로 함께 지워지지 않는다. 또 조회가 GuardianProfile을 경유하는 서브쿼리이므로,
     * 호출자는 profile이 제거되기 전에(= User.withdraw() 전에) 이 메서드를 호출해야 한다.
     *
     * <p>GuardianPointHistory는 guardian_point_id가 NOT NULL FK라 히스토리를 먼저 삭제한다.
     */
    @Transactional
    public void deleteUserPoints(Long userId) {
        guardianPointRepository.findByUserId(userId)
                .ifPresent(guardianPoint -> {
                    pointHistoryRepository.deleteByGuardianPointId(guardianPoint.getId());
                    guardianPointRepository.delete(guardianPoint);
                });
    }

    private Map<PointType, PointActivitySummary> getActivitySummaries(
            GuardianPoint guardianPoint
    ) {
        Map<PointType, PointActivitySummary> summaries =
                new EnumMap<>(PointType.class);

        pointHistoryRepository
                .summarizeByGuardianPointId(guardianPoint.getId())
                .forEach(summary -> summaries.put(summary.getPointType(), summary));

        return summaries;
    }

    private PointActivity toActivity(
            PointType pointType,
            PointActivitySummary summary
    ) {
        if (summary == null) {
            return PointActivity.of(pointType, 0L, 0L);
        }

        return PointActivity.of(
                pointType,
                summary.getEarnedPoint(),
                summary.getActivityCount()
        );
    }
}
