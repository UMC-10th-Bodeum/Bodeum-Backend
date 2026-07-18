package com.bodeum.domain.community.entity;

import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "community_post_disability_tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_community_post_disability_tag_post_type",
                columnNames = {"post_id", "disability_type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostDisabilityTag extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "community_post_disability_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "disability_type", nullable = false, length = 50)
    private DisabilityType disabilityType;

    @Builder
    private PostDisabilityTag(Post post, DisabilityType disabilityType) {
        this.post = post;
        this.disabilityType = disabilityType;
    }

    public static PostDisabilityTag create(Post post, DisabilityType disabilityType) {
        return PostDisabilityTag.builder()
                .post(post)
                .disabilityType(disabilityType)
                .build();
    }
}
