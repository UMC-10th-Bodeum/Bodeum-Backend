package com.bodeum.domain.point.dto.response;

import com.bodeum.domain.point.enums.PointType;
import java.util.List;

public record MyPointResponse(
        int totalPoint,
        List<PointActivity> activities
) {

    public MyPointResponse {
        activities = List.copyOf(activities);
    }

    public record PointActivity(
            PointType pointType,
            String label,
            int pointPerAction,
            long earnedPoint,
            long activityCount
    ) {

        public static PointActivity of(
                PointType pointType,
                long earnedPoint,
                long activityCount
        ) {
            return new PointActivity(
                    pointType,
                    pointType.getLabel(),
                    pointType.getPointPerAction(),
                    earnedPoint,
                    activityCount
            );
        }
    }
}
