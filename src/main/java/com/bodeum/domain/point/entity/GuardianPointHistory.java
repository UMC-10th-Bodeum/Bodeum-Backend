package com.bodeum.domain.point.entity;

import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "guardian_point_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_history_event_reference",
                columnNames = {"event_type", "reference_id", "actor_user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianPointHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guardian_point_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_point_id", nullable = false)
    private GuardianPoint guardianPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false, length = 50)
    private PointType pointType;

    @Column(name = "point_value", nullable = false)
    private Integer pointValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private PointEventType eventType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    private GuardianPointHistory(
            GuardianPoint guardianPoint,
            PointEventType eventType,
            Long referenceId,
            Long actorUserId
    ) {
        this.guardianPoint = Objects.requireNonNull(guardianPoint);
        this.eventType = Objects.requireNonNull(eventType);
        this.pointType = eventType.getPointType();
        this.pointValue = eventType.getPointType().getPointPerAction();
        this.referenceId = Objects.requireNonNull(referenceId);
        this.actorUserId = Objects.requireNonNull(actorUserId);
    }

    public static GuardianPointHistory create(
            GuardianPoint guardianPoint,
            PointEventType eventType,
            Long referenceId,
            Long actorUserId
    ) {
        return new GuardianPointHistory(
                guardianPoint,
                eventType,
                referenceId,
                actorUserId
        );
    }
}
