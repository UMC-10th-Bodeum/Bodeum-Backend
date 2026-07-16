package com.bodeum.domain.user.entity;

import com.bodeum.domain.user.enumtype.DisabilityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "child_disabilities")
public class ChildDisability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_profile_id", nullable = false)
    private ChildProfile childProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "disability_type", nullable = false, length = 50)
    private DisabilityType disabilityType;

    protected ChildDisability() {
    }

    private ChildDisability(ChildProfile childProfile, DisabilityType disabilityType) {
        this.childProfile = childProfile;
        this.disabilityType = disabilityType;
    }

    public static ChildDisability create(ChildProfile childProfile, DisabilityType disabilityType) {
        return new ChildDisability(childProfile, disabilityType);
    }

    public DisabilityType getDisabilityType() {
        return disabilityType;
    }
}
