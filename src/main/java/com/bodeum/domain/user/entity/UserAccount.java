package com.bodeum.domain.user.entity;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.onboarding.enumtype.CareArea;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.InterestCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "user_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_accounts_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(name = "uk_user_accounts_auth_subject", columnNames = "auth_subject")
        }
)
public class UserAccount {

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String nickname;

    @Column(name = "service_terms_agreed", nullable = false)
    private boolean serviceTermsAgreed;

    @Column(name = "privacy_policy_agreed", nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(name = "ai_chat_agreed", nullable = false)
    private boolean aiChatAgreed;

    @Column(name = "agreement_agreed_at")
    private LocalDateTime agreementAgreedAt;

    @Column(name = "child_name", length = 20)
    private String childName;

    @Column(name = "child_birth_year")
    private Integer childBirthYear;

    @Column(name = "child_birth_month")
    private Integer childBirthMonth;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_care_areas", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "care_area", nullable = false, length = 50)
    private List<CareArea> careAreas = new ArrayList<>();

    @Column(name = "characteristic_keyword", length = 100)
    private String characteristicKeyword;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", nullable = false, length = 50)
    private List<InterestCategory> interests = new ArrayList<>();

    @Column(name = "region_level_1", length = 50)
    private String regionLevel1;

    @Column(name = "region_level_2", length = 50)
    private String regionLevel2;

    @Column(name = "guardian_nickname", length = 20)
    private String guardianNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_type", length = 50)
    private GuardianType guardianType;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_role_type", length = 50)
    private CommunityRoleType communityRoleType;

    @Column(name = "onboarding_skipped", nullable = false)
    private boolean onboardingSkipped;

    @Column(nullable = false)
    private boolean withdrawn;

    protected UserAccount() {
    }

    private UserAccount(
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
        this.createdAt = LocalDateTime.now();
    }

    public static UserAccount createSocialUser(
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        return new UserAccount(provider, providerUserId, email, nickname);
    }

    @PrePersist
    void prePersist() {
        if (authSubject == null) {
            authSubject = UUID.randomUUID().toString();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void agreeTerms(boolean serviceTermsAgreed, boolean privacyPolicyAgreed, boolean aiChatAgreed) {
        this.serviceTermsAgreed = serviceTermsAgreed;
        this.privacyPolicyAgreed = privacyPolicyAgreed;
        this.aiChatAgreed = aiChatAgreed;
        this.agreementAgreedAt = LocalDateTime.now();
    }

    public void updateChildProfile(
            String childName,
            Integer childBirthYear,
            Integer childBirthMonth,
            List<CareArea> careAreas,
            String characteristicKeyword
    ) {
        this.childName = childName;
        this.childBirthYear = childBirthYear;
        this.childBirthMonth = childBirthMonth;
        this.careAreas = new ArrayList<>(careAreas);
        this.characteristicKeyword = characteristicKeyword;
    }

    public void updateInterestRegion(
            List<InterestCategory> interests,
            String regionLevel1,
            String regionLevel2
    ) {
        this.interests = new ArrayList<>(interests);
        this.regionLevel1 = regionLevel1;
        this.regionLevel2 = regionLevel2;
    }

    public void updateGuardianProfile(
            String guardianNickname,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        this.guardianNickname = guardianNickname;
        this.guardianType = guardianType;
        this.communityRoleType = communityRoleType;
        this.nickname = guardianNickname;
    }

    public void skipOnboarding() {
        this.onboardingSkipped = true;
    }

    public void updateProfile(
            String nickname,
            String childName,
            Integer childBirthYear,
            Integer childBirthMonth,
            List<CareArea> careAreas,
            String characteristicKeyword,
            List<InterestCategory> interests,
            String regionLevel1,
            String regionLevel2,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
            this.guardianNickname = nickname;
        }

        if (childName != null && !childName.isBlank()) {
            this.childName = childName;
        }

        if (childBirthYear != null) {
            this.childBirthYear = childBirthYear;
        }

        if (childBirthMonth != null) {
            this.childBirthMonth = childBirthMonth;
        }

        if (careAreas != null) {
            this.careAreas = new ArrayList<>(careAreas);
        }

        if (characteristicKeyword != null) {
            this.characteristicKeyword = blankToNull(characteristicKeyword);
        }

        if (interests != null) {
            this.interests = new ArrayList<>(interests);
        }

        if (regionLevel1 != null) {
            this.regionLevel1 = blankToNull(regionLevel1);
        }

        if (regionLevel2 != null) {
            this.regionLevel2 = blankToNull(regionLevel2);
        }

        if (guardianType != null) {
            this.guardianType = guardianType;
        }

        if (communityRoleType != null) {
            this.communityRoleType = communityRoleType;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public void withdraw() {
        this.withdrawn = true;
    }

    public boolean isAgreementCompleted() {
        return serviceTermsAgreed && privacyPolicyAgreed;
    }

    public boolean isChildProfileRegistered() {
        return childBirthYear != null && childBirthMonth != null && !careAreas.isEmpty();
    }

    public boolean isInterestRegionRegistered() {
        return !interests.isEmpty() && regionLevel1 != null && regionLevel2 != null;
    }

    public boolean isGuardianProfileRegistered() {
        // 보호자 유형/커뮤니티 성향은 선택 항목이므로 필수인 프로필 닉네임만으로 단계 완료를 판단한다.
        return guardianNickname != null;
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

    public Long getId() {
        return id;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getAuthSubject() {
        return authSubject;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isServiceTermsAgreed() {
        return serviceTermsAgreed;
    }

    public boolean isPrivacyPolicyAgreed() {
        return privacyPolicyAgreed;
    }

    public boolean isAiChatAgreed() {
        return aiChatAgreed;
    }

    public LocalDateTime getAgreementAgreedAt() {
        return agreementAgreedAt;
    }

    public String getChildName() {
        return childName;
    }

    public Integer getChildBirthYear() {
        return childBirthYear;
    }

    public Integer getChildBirthMonth() {
        return childBirthMonth;
    }

    public List<CareArea> getCareAreas() {
        return List.copyOf(careAreas);
    }

    public String getCharacteristicKeyword() {
        return characteristicKeyword;
    }

    public List<InterestCategory> getInterests() {
        return List.copyOf(interests);
    }

    public String getRegionLevel1() {
        return regionLevel1;
    }

    public String getRegionLevel2() {
        return regionLevel2;
    }

    public String getGuardianNickname() {
        return guardianNickname;
    }

    public GuardianType getGuardianType() {
        return guardianType;
    }

    public CommunityRoleType getCommunityRoleType() {
        return communityRoleType;
    }

    public boolean isOnboardingSkipped() {
        return onboardingSkipped;
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }
}
