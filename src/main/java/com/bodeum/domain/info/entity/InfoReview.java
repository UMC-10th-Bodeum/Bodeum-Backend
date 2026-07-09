package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_review")
public class InfoReview extends BaseCreatedUpdatedDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_review_id")
    private Long id;

//    # User 엔티티 구현 이후 FK 연결 예정
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_id", nullable = false)
    private InfoItem infoItem;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount = 0;

    @Builder
    public InfoReview(Long userId, InfoItem infoItem, BigDecimal rating, String content) {

        // User 엔티티 연결 이후 수정 예정
        this.userId = userId;

        this.infoItem = infoItem;
        this.rating = rating;
        this.content = content;
        this.helpfulCount = 0;
    }

    public void updateContent(String content, BigDecimal rating) {
        this.content = content;
        this.rating = rating;
    }

    public void updateHelpfulCount(int amount) {
        this.helpfulCount += amount;
    }
}
