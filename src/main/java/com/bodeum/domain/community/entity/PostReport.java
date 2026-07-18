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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport extends BaseCreatedEntity {

    public static final int REASON_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reason", nullable = false, length = REASON_MAX_LENGTH)
    private String reason;

    @Builder
    private PostReport(Post post, Long userId, String reason) {
        this.post = post;
        this.userId = userId;
        this.reason = reason;
    }

    public static PostReport create(Post post, Long userId, String reason) {
        return PostReport.builder()
                .post(post)
                .userId(userId)
                .reason(reason)
                .build();
    }
}
