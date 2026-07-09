package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "info_review_helpful",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "info_review_id"})
        }
)
public class InfoReviewHelpful extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_review_helpful_id")
    private Long id;

//    # User 엔티티 구현 이후 FK 연결 예정
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_review_id", nullable = false)
    private InfoReview infoReview;

    @Builder
    public InfoReviewHelpful(Long userId, InfoReview infoReview) {

        // User 엔티티 연결 이후 수정 예정
        this.userId = userId;

        this.infoReview = infoReview;
    }
}
