package com.bodeum.domain.mypage.entity;

import com.bodeum.domain.mypage.entity.enums.PointType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "guardian_point_history")
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
}
