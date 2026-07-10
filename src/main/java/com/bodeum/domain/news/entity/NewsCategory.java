package com.bodeum.domain.news.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "news_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_category_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "news_type", nullable = false, length = 30)
    private NewsType newsType;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}
