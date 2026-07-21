package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_category_id", nullable = false)
    private InfoCategory infoCategory;

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

    // --- 데모데이 및 기관 대표 이미지용 컬럼 ---
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "scrap_count", nullable = false)
    private int scrapCount = 0;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    // --- 태그 연관관계 매핑 ---
    @OneToMany(mappedBy = "infoItem")
    private List<InfoItemTag> infoItemTags = new ArrayList<>();

    @Builder
    public InfoItem(String externalId, InfoCategory infoCategory, String name, String introduction,
                    String address, String sido, String sigungu, String phone, String homepageUrl,
                    String imageUrl, LocalDateTime syncedAt) {
        this.externalId = externalId;
        this.infoCategory = infoCategory;
        this.name = name;
        this.introduction = introduction;
        this.address = address;
        this.sido = sido;
        this.sigungu = sigungu;
        this.phone = phone;
        this.homepageUrl = homepageUrl;
        this.imageUrl = imageUrl;
        this.syncedAt = syncedAt;
    }

    public void updateInformation(String name, InfoCategory infoCategory, String introduction, String address,
                                  String sido, String sigungu, String phone, String homepageUrl, String imageUrl) {
        this.name = name;
        this.infoCategory = infoCategory;
        this.introduction = introduction;
        this.address = address;
        this.sido = sido;
        this.sigungu = sigungu;
        this.phone = phone;
        this.homepageUrl = homepageUrl;
        this.imageUrl = imageUrl;

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

    // 1. 카테고리 명칭 반환 (InfoCategory 연관관계 활용)
    public List<String> getCategoryNames() {
        if (this.infoCategory == null) return List.of();
        return List.of(this.infoCategory.getMainCategoryKo(), this.infoCategory.getSubCategoryKo());
    }

    // 2. 전문 분야 태그 명칭 리스트 반환 (InfoItemTag -> InfoTag 매핑)
    public List<String> getTags() {
        if (this.infoItemTags == null || this.infoItemTags.isEmpty()) {
            return List.of();
        }
        return this.infoItemTags.stream()
                .map(itemTag -> itemTag.getInfoTag().getName())
                .toList();
    }

    // 3. 이미지 URL 리스트 반환 (이미지가 있으면 리스트에 담고, 없으면 빈 리스트 반환)
    public List<String> getImageUrls() {
        if (this.imageUrl != null && !this.imageUrl.isBlank()) {
            return List.of(this.imageUrl);
        }
        return List.of();
    }
}