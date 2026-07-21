package com.bodeum.domain.info.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_region")
public class InfoRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_region_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String sido;

    @Column(nullable = false, length = 50)
    private String sigungu;

    public InfoRegion(String sido, String sigungu) {
        this.sido = sido;
        this.sigungu = sigungu;
    }
}