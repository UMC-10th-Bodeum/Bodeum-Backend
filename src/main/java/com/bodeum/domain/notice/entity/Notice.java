package com.bodeum.domain.notice.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice")
public class Notice extends BaseCreatedUpdatedDeletedEntity {

    public static final int TITLE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

//    TODO: User 엔티티 구현 후 user_id FK(작성자) 적용 예정
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Builder
    private Notice(Long userId, String title, String content,
                   Boolean isActive, Instant startAt, Instant endAt) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.isActive = isActive != null ? isActive : true;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Notice create(Long userId, String title, String content,
                                Instant startAt, Instant endAt) {
        return Notice.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .isActive(true)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    public void deactivate() {
        this.isActive = false;
    }
}