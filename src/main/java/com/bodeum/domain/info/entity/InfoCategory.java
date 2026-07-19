package com.bodeum.domain.info.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_category")
public class InfoCategory {

    public static final int MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_category_id")
    private Long id;

    @Column(name = "parent_category", nullable = false, length = MAX_LENGTH)
    private String parentCategory;

    @Column(name = "parent_category_ko", nullable = false, length = MAX_LENGTH)
    private String parentCategoryKo;

    @Column(name = "sub_category", nullable = false, length = MAX_LENGTH)
    private String subCategory;

    @Column(name = "sub_category_ko", nullable = false, length = MAX_LENGTH)
    private String subCategoryKo;

    @Builder
    public InfoCategory(Long id, String parentCategory, String parentCategoryKo, String subCategory, String subCategoryKo) {
        this.id = id;
        this.parentCategory = parentCategory;
        this.parentCategoryKo = parentCategoryKo;
        this.subCategory = subCategory;
        this.subCategoryKo = subCategoryKo;
    }
}
