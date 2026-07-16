package com.bodeum.domain.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.auth.enums.AuthNextStep;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.enums.CommunityRoleType;
import com.bodeum.domain.onboarding.enums.GuardianType;
import com.bodeum.domain.onboarding.enums.OnboardingStep;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.service.RegionService;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.domain.user.service.UserService;
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

    @Mock
    private RegionService regionService;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void skipOnboardingRoutesToHomeWithoutCompletingOnboarding() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
        given(userService.getCurrentUser(1L)).willReturn(user);

        OnboardingStatusResponse response = onboardingService.skipOnboarding(1L);

        // 건너뛰기/그만하기는 온보딩을 "완료"로 만들지 않으면서(토스트 문구 분기 유지) 홈으로 보낸다.
        assertThat(user.isOnboardingSkipped()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.nextStep()).isEqualTo(AuthNextStep.HOME);
    }

    @Test
    void registerGuardianProfileStoresGuardianTypeAndCommunityRoleAsEnum() {
        User user = newUser();
        given(userService.getCurrentUser(1L)).willReturn(user);
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest(
                "민준맘",
                GuardianType.PARENT,
                CommunityRoleType.INFO_SEEKER
        );

        OnboardingStepResponse response = onboardingService.registerGuardianProfile(1L, request);

        assertThat(user.getGuardianType()).isEqualTo(GuardianType.PARENT);
        assertThat(user.getCommunityRoleType()).isEqualTo(CommunityRoleType.INFO_SEEKER);
        assertThat(user.getGuardianNickname()).isEqualTo("민준맘");
        assertThat(response.step()).isEqualTo(3);
        assertThat(response.completedStep()).isEqualTo(OnboardingStep.GUARDIAN_PROFILE);
    }

    @Test
    void registerGuardianProfileTreatsGuardianTypeAndCommunityRoleAsOptional() {
        User user = newUser();
        given(userService.getCurrentUser(1L)).willReturn(user);
        // 보호자 유형/커뮤니티 성향은 선택 항목 → 미입력(null)이어도 필수인 닉네임만 있으면 단계 완료로 인정한다.
        CreateGuardianProfileRequest request = new CreateGuardianProfileRequest("민준맘", null, null);

        onboardingService.registerGuardianProfile(1L, request);

        assertThat(user.getGuardianNickname()).isEqualTo("민준맘");
        assertThat(user.getGuardianType()).isNull();
        assertThat(user.getCommunityRoleType()).isNull();
        assertThat(user.isGuardianProfileRegistered()).isTrue();
    }

    @Test
    void registerChildProfileStoresDisabilityTypes() {
        User user = newUser();
        given(userService.getCurrentUser(1L)).willReturn(user);
        CreateChildProfileRequest request = new CreateChildProfileRequest(
                "민준이",
                "2020-01",
                List.of(
                        DisabilityType.AUTISM,
                        DisabilityType.ADHD,
                        DisabilityType.LANGUAGE_DISORDER,
                        DisabilityType.DEVELOPMENTAL_DELAY,
                        DisabilityType.ETC
                ),
                "소심함"
        );

        OnboardingStepResponse response = onboardingService.registerChildProfile(1L, request);

        assertThat(user.getChildBirth()).isEqualTo("2020-01");
        assertThat(user.getDisabilityTypes()).containsExactly(
                DisabilityType.AUTISM,
                DisabilityType.ADHD,
                DisabilityType.LANGUAGE_DISORDER,
                DisabilityType.DEVELOPMENTAL_DELAY,
                DisabilityType.ETC
        );
        assertThat(response.step()).isEqualTo(1);
        assertThat(response.completedStep()).isEqualTo(OnboardingStep.CHILD_PROFILE);
    }

    @Test
    void registerInterestRegionStoresInterestCategoriesAndRegion() {
        User user = newUser();
        Region region = Region.create("서울특별시", "강남구");
        given(userService.getCurrentUser(1L)).willReturn(user);
        given(regionService.getById(10L)).willReturn(region);
        CreateInterestRegionRequest request = new CreateInterestRegionRequest(
                List.of(
                        InterestCategory.INSTITUTION,
                        InterestCategory.HOSPITAL
                ),
                10L
        );

        OnboardingStepResponse response = onboardingService.registerInterestRegion(1L, request);

        assertThat(user.getInterestCategories()).containsExactly(
                InterestCategory.INSTITUTION,
                InterestCategory.HOSPITAL
        );
        assertThat(user.getRegion()).isEqualTo(region);
        assertThat(response.step()).isEqualTo(2);
        assertThat(response.completedStep()).isEqualTo(OnboardingStep.INTEREST_REGION);
    }

    private User newUser() {
        return User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
    }
}
