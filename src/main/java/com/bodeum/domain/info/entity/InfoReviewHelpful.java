package com.bodeum.domain.info.entity;

import com.bodeum.domain.user.entity.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_review_id", nullable = false)
    private InfoReview infoReview;

    @Builder
    public InfoReviewHelpful(User user, InfoReview infoReview) {
        this.user = user;
        this.infoReview = infoReview;
    }
}
