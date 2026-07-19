package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.enums.AiExternalSourceType;
import com.bodeum.domain.ai.enums.AiSourceAuthorityLevel;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ai_external_source",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_external_source_name_url",
                columnNames = {"name", "base_url"}
        )
)
public class AiExternalSource extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_external_source_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private AiExternalSourceType sourceType;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "entry_url", length = 1000)
    private String entryUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "authority_level", nullable = false, length = 30)
    private AiSourceAuthorityLevel authorityLevel;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Builder
    private AiExternalSource(
            String name,
            AiExternalSourceType sourceType,
            String baseUrl,
            String entryUrl,
            String description,
            AiSourceAuthorityLevel authorityLevel
    ) {
        this.name = name;
        this.sourceType = sourceType;
        this.baseUrl = baseUrl;
        this.entryUrl = blankToNull(entryUrl);
        this.description = description;
        this.authorityLevel = authorityLevel;
        this.active = true;
    }

    public static AiExternalSource create(
            String name,
            AiExternalSourceType sourceType,
            String baseUrl,
            String entryUrl,
            String description,
            AiSourceAuthorityLevel authorityLevel
    ) {
        return AiExternalSource.builder()
                .name(name)
                .sourceType(sourceType)
                .baseUrl(baseUrl)
                .entryUrl(entryUrl)
                .description(description)
                .authorityLevel(authorityLevel)
                .build();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
