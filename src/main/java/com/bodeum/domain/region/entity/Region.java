package com.bodeum.domain.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "regions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_regions_level",
                columnNames = {"region_level_1", "region_level_2"}
        )
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_level_1", nullable = false, length = 50)
    private String regionLevel1;

    @Column(name = "region_level_2", nullable = false, length = 50)
    private String regionLevel2;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    protected Region() {
    }

    private Region(String regionLevel1, String regionLevel2, String fullName) {
        this.regionLevel1 = regionLevel1;
        this.regionLevel2 = regionLevel2;
        this.fullName = fullName;
    }

    public static Region create(String regionLevel1, String regionLevel2) {
        return new Region(regionLevel1, regionLevel2, regionLevel1 + " " + regionLevel2);
    }

    public Long getId() {
        return id;
    }

    public String getRegionLevel1() {
        return regionLevel1;
    }

    public String getRegionLevel2() {
        return regionLevel2;
    }

    public String getFullName() {
        return fullName;
    }
}
