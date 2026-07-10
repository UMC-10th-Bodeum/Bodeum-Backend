package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import com.bodeum.domain.info.entity.enums.InfoCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_item")
public class InfoItem extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_item_id")
    private Long id;

    // API가 내려주는 고유 ID가 없을 경우, 응답값 조합으로 생성
    @Column(name = "external_id", nullable = false, length = 150)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfoCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 30)
    private String sido;

    @Column(nullable = false, length = 50)
    private String sigungu;

    @Column(length = 30)
    private String phone;

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "scrap_count", nullable = false)
    private int scrapCount = 0;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Builder
    public InfoItem(String externalId, InfoCategory category, String name, String introduction,
                    String address, String sido, String sigungu, String phone, String homepageUrl, LocalDateTime syncedAt) {
        this.externalId = externalId;
        this.category = category;
        this.name = name;
        this.introduction = introduction;
        this.address = address;
        this.sido = sido;
        this.sigungu = sigungu;
        this.phone = phone;
        this.homepageUrl = homepageUrl;
        this.syncedAt = syncedAt;
    }

    public void updateInformation(String name, String introduction, String address,
                                  String sido, String sigungu, String phone, String homepageUrl) {
        this.name = name;
        this.introduction = introduction;
        this.address = address;
        this.sido = sido;
        this.sigungu = sigungu;
        this.phone = phone;
        this.homepageUrl = homepageUrl;

        // 외부 API일 시 동기화를 위한 수정 메서드
        this.syncedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateScrapCount(int amount) {
        this.scrapCount += amount;
    }

    public void updateReviewCount(int amount) {
        this.reviewCount += amount;
    }
}
