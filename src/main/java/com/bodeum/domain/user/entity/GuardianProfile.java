package com.bodeum.domain.user.entity;

import com.bodeum.domain.onboarding.enums.CommunityRoleType;
import com.bodeum.domain.onboarding.enums.GuardianType;
import com.bodeum.domain.region.entity.Region;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "guardian_profiles")
public class GuardianProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 20)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_type", length = 50)
    private GuardianType guardianType;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_role_type", length = 50)
    private CommunityRoleType communityRoleType;

    @Column(nullable = false)
    private int point;

    protected GuardianProfile() {
    }

    private GuardianProfile(
            User user,
            String nickname,
            Region region,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        this.user = user;
        this.nickname = nickname;
        this.region = region;
        this.guardianType = guardianType;
        this.communityRoleType = communityRoleType;
    }

    public static GuardianProfile create(
            User user,
            String nickname,
            Region region,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        return new GuardianProfile(user, nickname, region, guardianType, communityRoleType);
    }

    public void updateGuardian(
            String nickname,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        this.nickname = nickname;
        this.guardianType = guardianType;
        this.communityRoleType = communityRoleType;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateRegion(Region region) {
        this.region = region;
    }

    public void updateGuardianType(GuardianType guardianType) {
        this.guardianType = guardianType;
    }

    public void updateCommunityRoleType(CommunityRoleType communityRoleType) {
        this.communityRoleType = communityRoleType;
    }

    public boolean isRegistered() {
        return nickname != null;
    }

    public String getNickname() {
        return nickname;
    }

    public Region getRegion() {
        return region;
    }

    public GuardianType getGuardianType() {
        return guardianType;
    }

    public CommunityRoleType getCommunityRoleType() {
        return communityRoleType;
    }

    public int getPoint() {
        return point;
    }
}
