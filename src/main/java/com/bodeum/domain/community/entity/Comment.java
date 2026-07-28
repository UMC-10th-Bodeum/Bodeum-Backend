package com.bodeum.domain.community.entity;

import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
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

    public static final int CONTENT_MAX_LENGTH = 1000;

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
    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH, columnDefinition = "TEXT")
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
        validateContent(content);

        this.post = post;
        this.userId = userId;
        this.parent = parent;
        this.content = content;
        this.status = CommentStatus.ACTIVE;
        this.post.increaseCommentCount();
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
        validateContent(content);
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

    @Override
    public void delete() {
        if (this.status == CommentStatus.DELETED) {
            return;
        }

        if (this.status == CommentStatus.ACTIVE) {
            this.post.decreaseCommentCount();
        }

        super.delete();
        this.status = CommentStatus.DELETED;
    }

    @Override
    public void restore() {
        if (this.status == CommentStatus.ACTIVE) {
            return;
        }

        super.restore();
        this.status = CommentStatus.ACTIVE;
        this.post.increaseCommentCount();
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CommunityException(CommunityErrorCode.COMMENT_CONTENT_REQUIRED);
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new CommunityException(CommunityErrorCode.COMMENT_CONTENT_TOO_LONG);
        }
    }
}
