package com.bodeum.domain.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.enumtype.CareArea;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.InterestCategory;
import com.bodeum.domain.onboarding.exception.OnboardingErrorCode;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void skipOnboardingRoutesToHomeWithoutCompletingOnboarding() {
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);

        OnboardingStatusResponse response = onboardingService.skipOnboarding(authentication);

        // 건너뛰기/그만하기는 온보딩을 "완료"로 만들지 않으면서(토스트 문구 분기 유지) 홈으로 보낸다.
        assertThat(userAccount.isOnboardingSkipped()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.nextStep()).isEqualTo(AuthNextStep.HOME);
    }

    @Test
    void registerGuardianProfileStoresGuardianTypeAndCommunityRoleAsEnum() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", "PARENT", "INFO_SEEKER");

        onboardingService.registerGuardianProfile(authentication, request);

        assertThat(userAccount.getGuardianType()).isEqualTo(GuardianType.PARENT);
        assertThat(userAccount.getCommunityRoleType()).isEqualTo(CommunityRoleType.INFO_SEEKER);
        assertThat(userAccount.getGuardianNickname()).isEqualTo("민준맘");
    }

    @Test
    void registerGuardianProfileTreatsGuardianTypeAndCommunityRoleAsOptional() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        // 보호자 유형/커뮤니티 성향은 선택 항목 → 미입력(null)이어도 필수인 닉네임만 있으면 단계 완료로 인정한다.
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", null, null);

        onboardingService.registerGuardianProfile(authentication, request);

        assertThat(userAccount.getGuardianNickname()).isEqualTo("민준맘");
        assertThat(userAccount.getGuardianType()).isNull();
        assertThat(userAccount.getCommunityRoleType()).isNull();
        assertThat(userAccount.isGuardianProfileRegistered()).isTrue();
    }

    @Test
    void registerGuardianProfileRejectsUnsupportedGuardianType() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", "이웃", "INFO_SEEKER");

        assertThatThrownBy(() -> onboardingService.registerGuardianProfile(authentication, request))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(OnboardingErrorCode.INVALID_GUARDIAN_TYPE);
    }

    @Test
    void registerChildProfileStoresCareAreasAsEnumWithoutCountLimit() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        CreateChildProfileRequest request = new CreateChildProfileRequest(
                "민준이",
                2020,
                1,
                List.of("AUTISM_SPECTRUM", "ADHD", "LANGUAGE", "DEVELOPMENTAL", "OTHER"),
                "소심함"
        );

        onboardingService.registerChildProfile(authentication, request);

        assertThat(userAccount.getCareAreas()).containsExactly(
                CareArea.AUTISM_SPECTRUM,
                CareArea.ADHD,
                CareArea.LANGUAGE,
                CareArea.DEVELOPMENTAL,
                CareArea.OTHER
        );
    }

    @Test
    void registerChildProfileRejectsUnsupportedCareArea() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        CreateChildProfileRequest request = new CreateChildProfileRequest(
                "민준이",
                2020,
                1,
                List.of("UNKNOWN"),
                null
        );

        assertThatThrownBy(() -> onboardingService.registerChildProfile(authentication, request))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(OnboardingErrorCode.INVALID_CARE_AREA);
    }

    @Test
    void registerInterestRegionStoresInterestsAsEnum() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        CreateInterestRegionRequest request = new CreateInterestRegionRequest(
                List.of("WELFARE_SUBSIDY", "GROWTH_EDUCATION"),
                "서울특별시",
                "강남구"
        );

        onboardingService.registerInterestRegion(authentication, request);

        assertThat(userAccount.getInterests())
                .containsExactly(InterestCategory.WELFARE_SUBSIDY, InterestCategory.GROWTH_EDUCATION);
        assertThat(userAccount.getRegionLevel1()).isEqualTo("서울특별시");
        assertThat(userAccount.getRegionLevel2()).isEqualTo("강남구");
    }

    @Test
    void registerInterestRegionRejectsUnsupportedInterest() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(authentication)).willReturn(userAccount);
        CreateInterestRegionRequest request = new CreateInterestRegionRequest(
                List.of("TRAVEL"),
                "서울특별시",
                "강남구"
        );

        assertThatThrownBy(() -> onboardingService.registerInterestRegion(authentication, request))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(OnboardingErrorCode.INVALID_INTEREST_CATEGORY);
    }

    private UserAccount newUserAccount() {
        return UserAccount.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
    }
}
