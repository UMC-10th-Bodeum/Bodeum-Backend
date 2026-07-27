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
