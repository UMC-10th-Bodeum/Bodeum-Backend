package com.bodeum.domain.community.entity;

import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Comment extends BaseCreatedUpdatedDeletedEntity {

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

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommentStatus status = CommentStatus.ACTIVE;

    @Builder
    private Comment(Post post, Long userId, Comment parent, String content) {
        this.post = post;
        this.userId = userId;
        this.parent = parent;
        this.content = content;
        this.status = CommentStatus.ACTIVE;
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

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void hide() {
        this.status = CommentStatus.HIDDEN;
    }

    @Override
    public void delete() {
        super.delete();
        this.status = CommentStatus.DELETED;
    }

    @Override
    public void restore() {
        super.restore();
        this.status = CommentStatus.ACTIVE;
    }
}
