package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.entity.enums.AiFeedbackReasonType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"ai_feedback_id", "reason"}
        )
)
public class AiFeedbackReason extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_feedback_reason_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_feedback_id", nullable = false)
    private AiFeedback aiFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private AiFeedbackReasonType reason;

    @Builder
    private AiFeedbackReason(
            AiFeedback aiFeedback,
            AiFeedbackReasonType reason
    ) {
        this.aiFeedback = aiFeedback;
        this.reason = reason;
    }

    public static AiFeedbackReason create(
            AiFeedback aiFeedback,
            AiFeedbackReasonType reason
    ) {
        return AiFeedbackReason.builder()
                .aiFeedback(aiFeedback)
                .reason(reason)
                .build();
    }
}
