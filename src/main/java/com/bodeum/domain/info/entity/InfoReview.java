package com.bodeum.domain.info.entity;

import com.bodeum.domain.user.entity.User;
import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_review")
public class InfoReview extends BaseCreatedUpdatedDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_item_id", nullable = false)
    private InfoItem infoItem;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount = 0;

    @OneToMany(mappedBy = "infoReview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC") // 순서대로 정렬 조회
    private List<InfoReviewImage> images = new ArrayList<>();

    @Builder
    public InfoReview(User user, InfoItem infoItem, int rating, String content) {

        this.user = user;

        this.infoItem = infoItem;
        this.rating = rating;
        this.content = content;
        this.helpfulCount = 0;
    }

    public void updateReview(String content, int rating, List<String> imageUrls) {
        this.content = content;
        this.rating = rating;

        // 이미지 변경 로직 (기존 영속 객체 clear 후 새 이미지 빌드)
        this.images.clear();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                this.images.add(InfoReviewImage.builder()
                        .infoReview(this)
                        .imageUrl(imageUrls.get(i))
                        .displayOrder(i)
                        .build());
            }
        }
    }

    public void updateHelpfulCount(int amount) {
        this.helpfulCount += amount;
    }
}
