package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_tag")
public class InfoTag extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_tag_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Builder
    public InfoTag(String name) {
        this.name = name;
    }
}
