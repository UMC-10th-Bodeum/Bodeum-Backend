package com.bodeum.domain.community.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "post_scrap",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_scrap_user_post",
                columnNames = {"user_id", "post_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostScrap extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_scrap_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    private PostScrap(Post post, Long userId) {
        this.post = post;
        this.userId = userId;
    }

    public static PostScrap create(Post post, Long userId) {
        return PostScrap.builder()
                .post(post)
                .userId(userId)
                .build();
    }
}
