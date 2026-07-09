package com.bodeum.domain.mypage.entity;

import com.bodeum.domain.mypage.entity.enums.BadgeLevel;
import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "guardian_profile_id", nullable = false)
    private Long guardianProfileId;

    /*
     * TODO: GuardianProfile 엔터티 연동 시 아래 연관관계로 변경 예정
     *
     * @OneToOne(fetch = FetchType.LAZY)
     * @JoinColumn(name = "guardian_profile_id", nullable = false)
     * private GuardianProfile guardianProfile;
     */

    @Column(name = "total_point", nullable = false)
    private Integer totalPoint = MIN_POINT;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_level", nullable = false, length = 30)
    private BadgeLevel badgeLevel = BadgeLevel.SPROUT;

    public void increasePoint(int point) {
        if (point <= MIN_POINT) {
            return;
        }

        this.totalPoint += point;
    }

    public void updateBadgeLevel(BadgeLevel badgeLevel) {
        this.badgeLevel = badgeLevel;
    }
}
