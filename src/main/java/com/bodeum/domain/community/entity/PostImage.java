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
        name = "post_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_image_post_sort_order",
                columnNames = {"post_id", "sort_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseCreatedEntity {

    public static final int IMAGE_URL_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "image_url", nullable = false, length = IMAGE_URL_MAX_LENGTH)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    private PostImage(Post post, String imageUrl, Integer sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public static PostImage create(Post post, String imageUrl, Integer sortOrder) {
        return PostImage.builder()
                .post(post)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }
}
