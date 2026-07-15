package com.bodeum.domain.user.entity;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.enumtype.DisabilityType;
import com.bodeum.domain.user.enumtype.GuardianLevel;
import com.bodeum.domain.user.enumtype.InterestCategory;
import com.bodeum.domain.user.enumtype.UserStatus;
import com.bodeum.global.common.entity.BaseCreatedUpdatedDeletedEntity;
import java.time.Instant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(name = "uk_users_auth_subject", columnNames = "auth_subject")
        }
)
public class User extends BaseCreatedUpdatedDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 128)
    private String providerUserId;

    @Column(name = "auth_subject", nullable = false, length = 36)
    private String authSubject;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Getter(AccessLevel.NONE)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserAgreement userAgreement;

    @Getter(AccessLevel.NONE)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ChildProfile childProfile;

    @Getter(AccessLevel.NONE)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private GuardianProfile guardianProfile;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterest> userInterests = new ArrayList<>();

    @Column(name = "onboarding_skipped", nullable = false)
    private boolean onboardingSkipped;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "withdrawal_reason", length = 255)
    private String withdrawalReason;

    protected User() {
    }

    private User(
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.authSubject = UUID.randomUUID().toString();
        this.email = email;
        this.nickname = nickname;
        this.status = UserStatus.ACTIVE;
    }

    public static User createSocialUser(
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        return new User(provider, providerUserId, email, nickname);
    }

    public void agreeTerms(boolean serviceTermsAgreed, boolean privacyPolicyAgreed, boolean aiTermsAgreed) {
        if (userAgreement == null) {
            userAgreement = UserAgreement.create(this, serviceTermsAgreed, privacyPolicyAgreed, aiTermsAgreed);
        } else {
            userAgreement.agree(serviceTermsAgreed, privacyPolicyAgreed, aiTermsAgreed);
        }
    }

    public void updateChildProfile(
            String childNickname,
            String childBirth,
            List<DisabilityType> disabilityTypes,
            String keywordText
    ) {
        if (childProfile == null) {
            childProfile = ChildProfile.create(this, childNickname, childBirth, disabilityTypes, keywordText);
        } else {
            childProfile.update(childNickname, childBirth, disabilityTypes, keywordText);
        }
    }

    public void updateInterestRegion(
            List<InterestCategory> interestCategories,
            Region region
    ) {
        replaceUserInterests(interestCategories);
        guardianProfile().updateRegion(region);
    }

    public void updateGuardianProfile(
            String guardianNickname,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        GuardianProfile profile = guardianProfile();
        profile.updateGuardian(guardianNickname, guardianType, communityRoleType);
        this.nickname = guardianNickname;
    }

    public void skipOnboarding() {
        this.onboardingSkipped = true;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(
            String nickname,
            String childNickname,
            String childBirth,
            List<DisabilityType> disabilityTypes,
            String keywordText,
            List<InterestCategory> interestCategories,
            Region region,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
            guardianProfile().updateNickname(nickname);
        }

        if (childNickname != null || childBirth != null || disabilityTypes != null || keywordText != null) {
            childProfile().updatePartial(childNickname, childBirth, disabilityTypes, keywordText);
        }

        if (interestCategories != null) {
            replaceUserInterests(interestCategories);
        }

        if (region != null) {
            guardianProfile().updateRegion(region);
        }

        if (guardianType != null) {
            guardianProfile().updateGuardianType(guardianType);
        }

        if (communityRoleType != null) {
            guardianProfile().updateCommunityRoleType(communityRoleType);
        }
    }

    private ChildProfile childProfile() {
        if (childProfile == null) {
            childProfile = ChildProfile.create(this, null, null, List.of(), null);
        }
        return childProfile;
    }

    private GuardianProfile guardianProfile() {
        if (guardianProfile == null) {
            guardianProfile = GuardianProfile.create(this, null, null, null, null);
        }
        return guardianProfile;
    }

    private void replaceUserInterests(List<InterestCategory> interestCategories) {
        userInterests.clear();
        interestCategories.forEach(interestCategory ->
                userInterests.add(UserInterest.create(this, interestCategory))
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public void withdraw(String reason) {
        this.status = UserStatus.DELETED;
        this.withdrawalReason = blankToNull(reason);
        delete();
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.withdrawalReason = null;
        restore();
    }

    public void hideByAdmin() {
        this.status = UserStatus.HIDDEN;
    }

    public boolean isAgreementCompleted() {
        return userAgreement != null && userAgreement.isRequiredAgreed();
    }

    public boolean isServiceTermsAgreed() {
        return userAgreement != null && userAgreement.isServiceTermsAgreed();
    }

    public boolean isPrivacyPolicyAgreed() {
        return userAgreement != null && userAgreement.isPrivacyPolicyAgreed();
    }

    public boolean isAiTermsAgreed() {
        return userAgreement != null && userAgreement.isAiTermsAgreed();
    }

    public Instant getAgreementAgreedAt() {
        return userAgreement == null ? null : userAgreement.getAgreedAt();
    }

    public boolean isChildProfileRegistered() {
        return childProfile != null && childProfile.isRegistered();
    }

    public boolean isInterestRegionRegistered() {
        return !userInterests.isEmpty() && getRegion() != null;
    }

    public boolean isGuardianProfileRegistered() {
        return guardianProfile != null && guardianProfile.isRegistered();
    }

    public boolean isOnboardingCompleted() {
        return isChildProfileRegistered() && isInterestRegionRegistered() && isGuardianProfileRegistered();
    }

    /**
     * 온보딩을 끝까지 마쳤거나(완료), 건너뛰기/그만하기로 스킵한 상태.
     * 둘 중 하나면 더 이상 온보딩으로 보내지 않고 홈으로 라우팅한다.
     */
    public boolean isOnboardingResolved() {
        return isOnboardingCompleted() || onboardingSkipped;
    }

    public String getChildName() {
        return childProfile == null ? null : childProfile.getNickname();
    }

    public String getChildBirth() {
        return childProfile == null ? null : childProfile.getBirth();
    }

    public Integer getChildAge() {
        return childProfile == null ? null : childProfile.getAge();
    }

    public List<DisabilityType> getDisabilityTypes() {
        return childProfile == null ? List.of() : childProfile.getDisabilityTypes();
    }

    public String getKeywordText() {
        return childProfile == null ? null : childProfile.getKeywordText();
    }

    public List<InterestCategory> getInterestCategories() {
        return userInterests.stream()
                .map(UserInterest::getInterestCategory)
                .toList();
    }

    public Region getRegion() {
        return guardianProfile == null ? null : guardianProfile.getRegion();
    }

    public String getGuardianNickname() {
        return guardianProfile == null ? null : guardianProfile.getNickname();
    }

    public GuardianType getGuardianType() {
        return guardianProfile == null ? null : guardianProfile.getGuardianType();
    }

    public CommunityRoleType getCommunityRoleType() {
        return guardianProfile == null ? null : guardianProfile.getCommunityRoleType();
    }

    public int getPoint() {
        return guardianProfile == null ? 0 : guardianProfile.getPoint();
    }

    public GuardianLevel getGuardianLevel() {
        return GuardianLevel.from(getPoint());
    }

    public boolean isWithdrawn() {
        return status == UserStatus.DELETED;
    }

    public boolean isHidden() {
        return status == UserStatus.HIDDEN;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
