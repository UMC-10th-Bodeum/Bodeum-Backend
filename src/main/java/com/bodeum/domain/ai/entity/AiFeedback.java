package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.enums.AiFeedbackType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_feedback")
public class AiFeedback extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_feedback_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_message_id", nullable = false, unique = true)
    private AiMessage aiMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private AiFeedbackType feedbackType;

    @Builder
    private AiFeedback(
            AiMessage aiMessage,
            AiFeedbackType feedbackType
    ) {
        this.aiMessage = aiMessage;
        this.feedbackType = feedbackType;
    }

    public static AiFeedback create(
            AiMessage aiMessage,
            AiFeedbackType feedbackType
    ) {
        return AiFeedback.builder()
                .aiMessage(aiMessage)
                .feedbackType(feedbackType)
                .build();
    }
}
