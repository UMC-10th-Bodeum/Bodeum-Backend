package com.bodeum.domain.user.model;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import java.time.LocalDateTime;
import java.util.List;

public class UserAccount {

    private final Long id;
    private final SocialProvider provider;
    private final String providerUserId;
    private final LocalDateTime createdAt;

    private String email;
    private String nickname;
    private boolean serviceTermsAgreed;
    private boolean privacyPolicyAgreed;
    private boolean marketingAgreed;
    private LocalDateTime agreementAgreedAt;
    private String childName;
    private Integer childBirthYear;
    private Integer childBirthMonth;
    private List<String> careAreas = List.of();
    private String characteristicKeyword;
    private List<String> interests = List.of();
    private String sido;
    private String sigungu;
    private String guardianNickname;
    private String guardianType;
    private String communityRoleType;
    private boolean withdrawn;

    private UserAccount(
            Long id,
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        this.id = id;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }

    public static UserAccount createSocialUser(
            Long id,
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        return new UserAccount(id, provider, providerUserId, email, nickname);
    }

    public void agreeTerms(boolean serviceTermsAgreed, boolean privacyPolicyAgreed, boolean marketingAgreed) {
        this.serviceTermsAgreed = serviceTermsAgreed;
        this.privacyPolicyAgreed = privacyPolicyAgreed;
        this.marketingAgreed = marketingAgreed;
        this.agreementAgreedAt = LocalDateTime.now();
    }

    public void updateChildProfile(
            String childName,
            Integer childBirthYear,
            Integer childBirthMonth,
            List<String> careAreas,
            String characteristicKeyword
    ) {
        this.childName = childName;
        this.childBirthYear = childBirthYear;
        this.childBirthMonth = childBirthMonth;
        this.careAreas = List.copyOf(careAreas);
        this.characteristicKeyword = characteristicKeyword;
    }

    public void updateInterestRegion(List<String> interests, String sido, String sigungu) {
        this.interests = List.copyOf(interests);
        this.sido = sido;
        this.sigungu = sigungu;
    }

    public void updateGuardianProfile(String guardianNickname, String guardianType, String communityRoleType) {
        this.guardianNickname = guardianNickname;
        this.guardianType = guardianType;
        this.communityRoleType = communityRoleType;
        this.nickname = guardianNickname;
    }

    public void updateProfile(String nickname, String childName, String guardianType) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
            this.guardianNickname = nickname;
        }

        if (childName != null && !childName.isBlank()) {
            this.childName = childName;
        }

        if (guardianType != null && !guardianType.isBlank()) {
            this.guardianType = guardianType;
        }
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
        return !interests.isEmpty() && sido != null && sigungu != null;
    }

    public boolean isGuardianProfileRegistered() {
        return guardianNickname != null && guardianType != null && communityRoleType != null;
    }

    public boolean isOnboardingCompleted() {
        return isChildProfileRegistered() && isInterestRegionRegistered() && isGuardianProfileRegistered();
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

    public boolean isMarketingAgreed() {
        return marketingAgreed;
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

    public List<String> getCareAreas() {
        return careAreas;
    }

    public String getCharacteristicKeyword() {
        return characteristicKeyword;
    }

    public List<String> getInterests() {
        return interests;
    }

    public String getSido() {
        return sido;
    }

    public String getSigungu() {
        return sigungu;
    }

    public String getGuardianNickname() {
        return guardianNickname;
    }

    public String getGuardianType() {
        return guardianType;
    }

    public String getCommunityRoleType() {
        return communityRoleType;
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }
}
