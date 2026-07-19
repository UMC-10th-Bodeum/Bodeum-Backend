package com.bodeum.domain.ai.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "ai_external_resource",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_external_resource_url_hash",
                columnNames = "source_url_hash"
        )
)
public class AiExternalResource extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_external_resource_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_external_source_id", nullable = false)
    private AiExternalSource externalSource;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_url_hash", nullable = false, length = 64)
    private String sourceUrlHash;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Builder
    private AiExternalResource(
            AiExternalSource externalSource,
            String title,
            String sourceUrl,
            String sourceUrlHash,
            Instant sourceUpdatedAt
    ) {
        this.externalSource = externalSource;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.sourceUrlHash = sourceUrlHash;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public static AiExternalResource create(
            AiExternalSource externalSource,
            String title,
            String sourceUrl,
            String sourceUrlHash,
            Instant sourceUpdatedAt
    ) {
        return AiExternalResource.builder()
                .externalSource(externalSource)
                .title(title)
                .sourceUrl(sourceUrl)
                .sourceUrlHash(sourceUrlHash)
                .sourceUpdatedAt(sourceUpdatedAt)
                .build();
    }

    public void updateReference(String title, String sourceUrl, Instant sourceUpdatedAt) {
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }
}
