package com.bodeum.domain.info.entity;

import com.bodeum.domain.info.entity.enums.MainCategory;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "main_category", nullable = false, length = MAX_LENGTH)
    private MainCategory mainCategory;

    @Column(name = "main_category_ko", nullable = false, length = MAX_LENGTH)
    private String mainCategoryKo;

    @Column(name = "sub_category", nullable = false, length = MAX_LENGTH)
    private String subCategory;

    @Column(name = "sub_category_ko", nullable = false, length = MAX_LENGTH)
    private String subCategoryKo;

    @Builder
    public InfoCategory(Long id, MainCategory mainCategory, String mainCategoryKo, String subCategory, String subCategoryKo) {
        this.id = id;
        this.mainCategory = mainCategory;
        this.mainCategoryKo = mainCategoryKo;
        this.subCategory = subCategory;
        this.subCategoryKo = subCategoryKo;
    }
}
