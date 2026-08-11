package com.bodeum.domain.news.entity;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "news",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_news_source_external_item",
                        columnNames = {"news_source_id", "external_item_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class News extends BaseCreatedUpdatedDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_category_id", nullable = false)
    private NewsCategory newsCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_source_id")
    private NewsSource newsSource;

    @Column(name = "external_item_id", length = 100)
    private String externalItemId;

    @Column(name = "region_id")
    private Long regionId;

    /*
     * TODO: Region 엔터티 연동 시 아래 연관관계로 변경 예정
     *
     * @ManyToOne(fetch = FetchType.LAZY)
     * @JoinColumn(name = "region_id")
     * private Region region;
     */

    @Column(name = "organization_id", length = 100)
    private String organizationId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "original_url", length = 500)
    private String originalUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "news_type", nullable = false, length = 30)
    private NewsType newsType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", length = 30)
    private RecruitmentStatus recruitmentStatus;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "scrap_count", nullable = false)
    private Long scrapCount = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "target_audience", length = 500)
    private String targetAudience;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "manager", length = 100)
    private String manager;

    @Column(name = "program_start_date")
    private LocalDate programStartDate;

    @Column(name = "program_end_date")
    private LocalDate programEndDate;

    @Column(name = "apply_start_date")
    private LocalDate applyStartDate;

    @Column(name = "apply_end_date")
    private LocalDate applyEndDate;

    @Builder(access = AccessLevel.PRIVATE)
    private News(
            NewsCategory newsCategory,
            NewsSource newsSource,
            Long regionId,
            NewsCandidate candidate
    ) {
        this.newsCategory = newsCategory;
        this.newsSource = newsSource;
        this.regionId = regionId;
        this.viewCount = 0L;
        this.scrapCount = 0L;
        this.active = true;
        apply(candidate);
    }

    public static News create(
            NewsCategory newsCategory,
            NewsSource newsSource,
            Long regionId,
            NewsCandidate candidate
    ) {
        return News.builder()
                .newsCategory(newsCategory)
                .newsSource(newsSource)
                .regionId(regionId)
                .candidate(candidate)
                .build();
    }

    public void updateCollectedData(
            NewsCategory newsCategory,
            NewsSource newsSource,
            Long regionId,
            NewsCandidate candidate
    ) {
        this.newsCategory = newsCategory;
        this.newsSource = newsSource;
        this.regionId = regionId;
        this.active = true;
        restore();
        apply(candidate);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseScrapCount() {
        this.scrapCount++;
    }

    public void decreaseScrapCount() {
        if (this.scrapCount > 0) {
            this.scrapCount--;
        }
    }

    public boolean isVisible() {
        return Boolean.TRUE.equals(this.active) && !isDeleted();
    }

    private void apply(NewsCandidate candidate) {
        this.externalItemId = candidate.externalItemId();
        this.title = candidate.title();
        this.summary = candidate.summary();
        this.content = candidate.content();
        this.sourceName = candidate.sourceName();
        this.publishedAt = candidate.publishedAt();
        String resolvedUrl = NewsExternalLinkResolver.resolve(
                candidate.sourceName(),
                candidate.title()
        );
        this.originalUrl = resolvedUrl != null ? resolvedUrl : candidate.originalUrl();
        this.thumbnailUrl = candidate.thumbnailUrl();
        this.newsType = candidate.newsType();
        this.recruitmentStatus = candidate.recruitmentStatus();
        this.targetAudience = candidate.targetAudience();
        this.contact = candidate.contact();
        this.manager = candidate.manager();
        this.programStartDate = candidate.programStartDate();
        this.programEndDate = candidate.programEndDate();
        this.applyStartDate = candidate.applyStartDate();
        this.applyEndDate = candidate.applyEndDate();
    }
}
