package com.bodeum.domain.news.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "news")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class News extends BaseCreatedUpdatedDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_category_id", nullable = false)
    private NewsCategory newsCategory;

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
}
