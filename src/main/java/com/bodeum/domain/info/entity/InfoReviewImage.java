package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_review_image")
public class InfoReviewImage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_review_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_review_id", nullable = false)
    private InfoReview infoReview;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder; // 이미지 표시 순서 (0, 1, 2...)

    @Builder
    public InfoReviewImage(InfoReview infoReview, String imageUrl, int displayOrder) {
        this.infoReview = infoReview;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}