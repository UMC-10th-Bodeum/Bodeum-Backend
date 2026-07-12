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
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.OnboardingStep;
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

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserService userService;

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
        given(userService.getCurrentUser(1L)).willReturn(userAccount);

        OnboardingStatusResponse response = onboardingService.skipOnboarding(1L);

        // 건너뛰기/그만하기는 온보딩을 "완료"로 만들지 않으면서(토스트 문구 분기 유지) 홈으로 보낸다.
        assertThat(userAccount.isOnboardingSkipped()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.nextStep()).isEqualTo(AuthNextStep.HOME);
    }

    @Test
    void registerGuardianProfileStoresGuardianTypeAndCommunityRoleAsEnum() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(1L)).willReturn(userAccount);
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", "PARENT", "INFO_SEEKER");

        OnboardingStepResponse response = onboardingService.registerGuardianProfile(1L, request);

        assertThat(userAccount.getGuardianType()).isEqualTo(GuardianType.PARENT);
        assertThat(userAccount.getCommunityRoleType()).isEqualTo(CommunityRoleType.INFO_SEEKER);
        assertThat(userAccount.getGuardianNickname()).isEqualTo("민준맘");
        assertThat(response.step()).isEqualTo(3);
        assertThat(response.completedStep()).isEqualTo(OnboardingStep.GUARDIAN_PROFILE);
    }

    @Test
    void registerGuardianProfileTreatsGuardianTypeAndCommunityRoleAsOptional() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(1L)).willReturn(userAccount);
        // 보호자 유형/커뮤니티 성향은 선택 항목 → 미입력(null)이어도 필수인 닉네임만 있으면 단계 완료로 인정한다.
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", null, null);

        onboardingService.registerGuardianProfile(1L, request);

        assertThat(userAccount.getGuardianNickname()).isEqualTo("민준맘");
        assertThat(userAccount.getGuardianType()).isNull();
        assertThat(userAccount.getCommunityRoleType()).isNull();
        assertThat(userAccount.isGuardianProfileRegistered()).isTrue();
    }

    @Test
    void registerGuardianProfileRejectsUnsupportedGuardianType() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(1L)).willReturn(userAccount);
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", "이웃", "INFO_SEEKER");

        assertThatThrownBy(() -> onboardingService.registerGuardianProfile(1L, request))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(OnboardingErrorCode.INVALID_GUARDIAN_TYPE);
    }

    @Test
    void registerChildProfileStoresDisabilityTypeIds() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(1L)).willReturn(userAccount);
        CreateChildProfileRequest request = new CreateChildProfileRequest(
                "민준이",
                "2020-01",
                List.of(1, 4, 6, 5, 7),
                "소심함"
        );

        OnboardingStepResponse response = onboardingService.registerChildProfile(1L, request);

        assertThat(userAccount.getChildBirth()).isEqualTo("2020-01");
        assertThat(userAccount.getDisabilityTypeIds()).containsExactly(1, 4, 6, 5, 7);
        assertThat(response.step()).isEqualTo(1);
        assertThat(response.completedStep()).isEqualTo(OnboardingStep.CHILD_PROFILE);
    }

    @Test
    void registerInterestRegionStoresInterestCategoryIds() {
        UserAccount userAccount = newUserAccount();
        given(userService.getCurrentUser(1L)).willReturn(userAccount);
        CreateInterestRegionRequest request = new CreateInterestRegionRequest(
                List.of(1, 2, 3),
                "서울특별시",
                "강남구"
        );

        OnboardingStepResponse response = onboardingService.registerInterestRegion(1L, request);

        assertThat(userAccount.getInterestCategoryIds()).containsExactly(1, 2, 3);
        assertThat(userAccount.getRegionLevel1()).isEqualTo("서울특별시");
        assertThat(userAccount.getRegionLevel2()).isEqualTo("강남구");
        assertThat(response.step()).isEqualTo(2);
        assertThat(response.completedStep()).isEqualTo(OnboardingStep.INTEREST_REGION);
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
