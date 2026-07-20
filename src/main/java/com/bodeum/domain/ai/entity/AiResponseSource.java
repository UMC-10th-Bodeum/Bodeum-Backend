package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_response_source")
public class AiResponseSource extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_response_source_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_message_id", nullable = false)
    private AiMessage aiMessage;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 20,
            columnDefinition = "VARCHAR(20)"
    )
    private AiResponseSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_title", nullable = false, length = 200)
    private String sourceTitle;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_updated_at", nullable = true)
    private Instant sourceUpdatedAt;

    @Builder
    private AiResponseSource(
            AiMessage aiMessage,
            AiResponseSourceType sourceType,
            Long sourceId,
            String sourceTitle,
            String sourceUrl,
            Instant sourceUpdatedAt
    ) {
        this.aiMessage = aiMessage;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public static AiResponseSource create(
            AiMessage aiMessage,
            AiResponseSourceType sourceType,
            Long sourceId,
            String sourceTitle,
            String sourceUrl,
            Instant sourceUpdatedAt
    ) {
        return AiResponseSource.builder()
                .aiMessage(aiMessage)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .sourceTitle(sourceTitle)
                .sourceUrl(sourceUrl)
                .sourceUpdatedAt(sourceUpdatedAt)
                .build();
    }
}
