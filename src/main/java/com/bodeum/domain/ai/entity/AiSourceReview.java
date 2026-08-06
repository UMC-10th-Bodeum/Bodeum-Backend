package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiSourceReviewStatus;
import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ai_source_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_source_review_source",
                columnNames = {"source_type", "source_id"}
        )
)
public class AiSourceReview extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_source_review_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private AiResponseSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private AiSourceReviewStatus reviewStatus;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Builder
    private AiSourceReview(
            AiResponseSourceType sourceType,
            Long sourceId,
            AiSourceReviewStatus reviewStatus,
            String reviewNote,
            Instant reviewedAt
    ) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.reviewStatus = reviewStatus;
        this.reviewNote = reviewNote;
        this.reviewedAt = reviewedAt;
    }

    public static AiSourceReview create(
            AiResponseSourceType sourceType,
            Long sourceId,
            AiSourceReviewStatus reviewStatus,
            String reviewNote
    ) {
        return AiSourceReview.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .reviewStatus(reviewStatus)
                .reviewNote(blankToNull(reviewNote))
                .reviewedAt(Instant.now())
                .build();
    }

    public void updateReview(AiSourceReviewStatus reviewStatus, String reviewNote) {
        this.reviewStatus = reviewStatus;
        this.reviewNote = blankToNull(reviewNote);
        this.reviewedAt = Instant.now();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
