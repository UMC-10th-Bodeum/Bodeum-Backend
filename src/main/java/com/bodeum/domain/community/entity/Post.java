package com.bodeum.domain.community.entity;

import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseCreatedUpdatedDeletedEntity {

    public static final int TITLE_MAX_LENGTH = 150;
    public static final int CONTENT_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 40)
    private PostBoardType boardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "anonymity_type", nullable = false, length = 30)
    private PostAnonymityType anonymityType;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Column(name = "is_question", nullable = false)
    private boolean question;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "scrap_count", nullable = false)
    private int scrapCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status = PostStatus.ACTIVE;

    @Builder
    private Post(
            Long userId,
            PostBoardType boardType,
            PostAnonymityType anonymityType,
            String title,
            String content,
            boolean question
    ) {
        validateTitle(title);
        validateContent(content);

        this.userId = userId;
        this.boardType = boardType;
        this.anonymityType = anonymityType;
        this.title = title;
        this.content = content;
        this.question = question;
        this.status = PostStatus.ACTIVE;
    }

    public static Post create(
            Long userId,
            PostBoardType boardType,
            PostAnonymityType anonymityType,
            String title,
            String content,
            boolean question
    ) {
        return Post.builder()
                .userId(userId)
                .boardType(boardType)
                .anonymityType(anonymityType)
                .title(title)
                .content(content)
                .question(question)
                .build();
    }

    public void update(
            PostBoardType boardType,
            PostAnonymityType anonymityType,
            String title,
            String content,
            boolean question
    ) {
        validateTitle(title);
        validateContent(content);

        this.boardType = boardType;
        this.anonymityType = anonymityType;
        this.title = title;
        this.content = content;
        this.question = question;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        this.commentCount = Math.max(0, this.commentCount - 1);
    }

    public void increaseScrapCount() {
        this.scrapCount++;
    }

    public void decreaseScrapCount() {
        this.scrapCount = Math.max(0, this.scrapCount - 1);
    }

    public void hide() {
        this.status = PostStatus.HIDDEN;
    }

    @Override
    public void delete() {
        super.delete();
        this.status = PostStatus.DELETED;
    }

    @Override
    public void restore() {
        super.restore();
        this.status = PostStatus.ACTIVE;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new CommunityException(CommunityErrorCode.POST_TITLE_REQUIRED);
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new CommunityException(CommunityErrorCode.POST_TITLE_TOO_LONG);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CommunityException(CommunityErrorCode.POST_CONTENT_REQUIRED);
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new CommunityException(CommunityErrorCode.POST_CONTENT_TOO_LONG);
        }
    }
}
