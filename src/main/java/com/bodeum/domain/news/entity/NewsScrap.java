package com.bodeum.domain.news.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "news_scrap",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_news_scrap_user_news",
                        columnNames = {"user_id", "news_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsScrap extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    @Column(name = "news_scrap_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /*
     * TODO: User 엔터티 연동 시 아래 연관관계로 변경 예정
     *
     * @ManyToOne(fetch = FetchType.LAZY)
     * @JoinColumn(name = "user_id", nullable = false)
     * private User user;
     */
}
