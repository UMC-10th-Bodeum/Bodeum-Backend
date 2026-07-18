package com.bodeum.domain.community.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_accepted", nullable = false)
    private boolean accepted = false;

    @Builder
    private Comment(Post post, Long userId, Comment parent, String content) {
        this.post = post;
        this.userId = userId;
        this.parent = parent;
        this.content = content;
    }

    public static Comment create(Post post, Long userId, String content) {
        return Comment.builder()
                .post(post)
                .userId(userId)
                .content(content)
                .build();
    }

    public static Comment createReply(Comment parent, Long userId, String content) {
        return Comment.builder()
                .post(parent.getPost())
                .userId(userId)
                .parent(parent)
                .content(content)
                .build();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void accept() {
        this.accepted = true;
    }
}
