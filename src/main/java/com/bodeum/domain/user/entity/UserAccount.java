package com.bodeum.domain.user.entity;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.user.enumtype.GuardianLevel;
import com.bodeum.domain.user.enumtype.UserStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "service_terms_agreed", nullable = false)
    private boolean serviceTermsAgreed;

    @Column(name = "privacy_policy_agreed", nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(name = "ai_terms_agreed", nullable = false)
    private boolean aiTermsAgreed;

    @Column(name = "agreement_agreed_at")
    private LocalDateTime agreementAgreedAt;

    @Column(name = "child_nickname", length = 20)
    private String childNickname;

    @Column(name = "child_birth", length = 7)
    private String childBirth;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_disability_types", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "disability_type_id", nullable = false)
    private List<Integer> disabilityTypeIds = new ArrayList<>();

    @Column(name = "keyword_text", length = 100)
    private String keywordText;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest_category_id", nullable = false)
    private List<Integer> interestCategoryIds = new ArrayList<>();

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
    private int point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "withdrawal_reason", length = 255)
    private String withdrawalReason;

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
        this.status = UserStatus.ACTIVE;
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

        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    public void agreeTerms(boolean serviceTermsAgreed, boolean privacyPolicyAgreed, boolean aiTermsAgreed) {
        this.serviceTermsAgreed = serviceTermsAgreed;
        this.privacyPolicyAgreed = privacyPolicyAgreed;
        this.aiTermsAgreed = aiTermsAgreed;
        this.agreementAgreedAt = LocalDateTime.now();
    }

    public void updateChildProfile(
            String childNickname,
            String childBirth,
            List<Integer> disabilityTypeIds,
            String keywordText
    ) {
        this.childNickname = childNickname;
        this.childBirth = childBirth;
        this.disabilityTypeIds = new ArrayList<>(disabilityTypeIds);
        this.keywordText = keywordText;
    }

    public void updateInterestRegion(
            List<Integer> interestCategoryIds,
            String regionLevel1,
            String regionLevel2
    ) {
        this.interestCategoryIds = new ArrayList<>(interestCategoryIds);
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

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(
            String nickname,
            String childNickname,
            String childBirth,
            List<Integer> disabilityTypeIds,
            String keywordText,
            List<Integer> interestCategoryIds,
            String regionLevel1,
            String regionLevel2,
            GuardianType guardianType,
            CommunityRoleType communityRoleType
    ) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
            this.guardianNickname = nickname;
        }

        if (childNickname != null && !childNickname.isBlank()) {
            this.childNickname = childNickname;
        }

        if (childBirth != null) {
            this.childBirth = childBirth;
        }

        if (disabilityTypeIds != null) {
            this.disabilityTypeIds = new ArrayList<>(disabilityTypeIds);
        }

        if (keywordText != null) {
            this.keywordText = blankToNull(keywordText);
        }

        if (interestCategoryIds != null) {
            this.interestCategoryIds = new ArrayList<>(interestCategoryIds);
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

    public void withdraw(String reason) {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.withdrawalReason = blankToNull(reason);
    }

    public boolean isAgreementCompleted() {
        return serviceTermsAgreed && privacyPolicyAgreed;
    }

    public boolean isChildProfileRegistered() {
        return childBirth != null && !disabilityTypeIds.isEmpty();
    }

    public boolean isInterestRegionRegistered() {
        return !interestCategoryIds.isEmpty() && regionLevel1 != null && regionLevel2 != null;
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public boolean isServiceTermsAgreed() {
        return serviceTermsAgreed;
    }

    public boolean isPrivacyPolicyAgreed() {
        return privacyPolicyAgreed;
    }

    public boolean isAiTermsAgreed() {
        return aiTermsAgreed;
    }

    public LocalDateTime getAgreementAgreedAt() {
        return agreementAgreedAt;
    }

    public String getChildName() {
        return childNickname;
    }

    public String getChildBirth() {
        return childBirth;
    }

    /**
     * 자녀 출생 연월 기준 만 나이. 생월이 아직 지나지 않았으면 한 살을 뺀다.
     * 생년월이 없으면 나이를 계산할 수 없으므로 null을 반환한다.
     */
    public Integer getChildAge() {
        if (childBirth == null) {
            return null;
        }

        YearMonth birth;
        try {
            birth = YearMonth.parse(childBirth);
        } catch (DateTimeParseException e) {
            // 저장 시 검증을 거치지만, 마이그레이션·수작업 수정 등으로 형식이 깨진 데이터가
            // 조회 API를 500으로 터뜨리지 않도록 나이를 계산 불가로 처리한다.
            return null;
        }

        LocalDate now = LocalDate.now();
        int age = now.getYear() - birth.getYear();
        if (now.getMonthValue() < birth.getMonthValue()) {
            age--;
        }

        return age;
    }

    public List<Integer> getDisabilityTypeIds() {
        return List.copyOf(disabilityTypeIds);
    }

    public String getKeywordText() {
        return keywordText;
    }

    public List<Integer> getInterestCategoryIds() {
        return List.copyOf(interestCategoryIds);
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

    public int getPoint() {
        return point;
    }

    public GuardianLevel getGuardianLevel() {
        return GuardianLevel.from(point);
    }

    public boolean isWithdrawn() {
        return status == UserStatus.DELETED;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }
}
