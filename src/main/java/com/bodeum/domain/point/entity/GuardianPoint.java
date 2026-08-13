package com.bodeum.domain.point.entity;

import com.bodeum.domain.point.enums.BadgeLevel;
import com.bodeum.domain.user.entity.GuardianProfile;
import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(
        name = "guardian_point",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_guardian_point_guardian_profile",
                        columnNames = "guardian_profile_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianPoint extends BaseCreatedUpdatedEntity {

    private static final int MIN_POINT = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guardian_point_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "guardian_profile_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_guardian_point_guardian_profile")
    )
    private GuardianProfile guardianProfile;

    @Column(name = "total_point", nullable = false)
    private Integer totalPoint = MIN_POINT;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_level", nullable = false, length = 30)
    private BadgeLevel badgeLevel = BadgeLevel.SPROUT;

    private GuardianPoint(GuardianProfile guardianProfile) {
        this.guardianProfile = Objects.requireNonNull(guardianProfile);
        this.totalPoint = MIN_POINT;
        this.badgeLevel = BadgeLevel.SPROUT;
    }

    public static GuardianPoint create(GuardianProfile guardianProfile) {
        return new GuardianPoint(guardianProfile);
    }

    public Long getGuardianProfileId() {
        return guardianProfile.getId();
    }

    public void increasePoint(int point) {
        if (point <= MIN_POINT) {
            return;
        }

        this.totalPoint += point;
    }

    public void decreasePoint(int point) {
        if (point <= MIN_POINT) {
            return;
        }

        this.totalPoint = Math.max(MIN_POINT, this.totalPoint - point);
    }

    public void updateBadgeLevel(BadgeLevel badgeLevel) {
        this.badgeLevel = badgeLevel;
    }
}
