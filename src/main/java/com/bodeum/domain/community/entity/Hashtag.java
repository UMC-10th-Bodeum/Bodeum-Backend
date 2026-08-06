package com.bodeum.domain.community.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "hashtag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hashtag_name",
                columnNames = "name"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hashtag extends BaseCreatedEntity {

    public static final int NAME_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Builder
    private Hashtag(String name) {
        this.name = name;
    }

    public static Hashtag create(String name) {
        return Hashtag.builder()
                .name(name)
                .build();
    }
}
