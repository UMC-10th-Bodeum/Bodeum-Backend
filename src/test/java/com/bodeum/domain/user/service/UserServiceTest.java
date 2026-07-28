package com.bodeum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.auth.repository.AuthLoginCodeRepository;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.region.service.RegionService;
import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.WithdrawUserRequest;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserHeaderResponse;
import com.bodeum.domain.user.dto.response.UserWithdrawResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.UserStatus;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Mock
    private S3ImageStorage s3ImageStorage;

    @Mock
    private UserProfileImageUpdater userProfileImageUpdater;

    @Mock
    private RegionService regionService;

    @Mock
    private AuthLoginCodeRepository authLoginCodeRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void headerInfoReturnsLoggedOutWhenUserIdIsNull() {
        // 헤더/사이드바 조회는 비로그인(인증 없음)이어도 예외 없이 비로그인 응답을 반환한다.
        UserHeaderResponse response = userService.getHeaderInfo(null);

        assertThat(response.isLoggedIn()).isFalse();
    }

    @Test
    void headerInfoReturnsUserInfoWhenLoggedIn() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserHeaderResponse response = userService.getHeaderInfo(1L);

        assertThat(response.isLoggedIn()).isTrue();
        assertThat(response.nickname()).isEqualTo("민준맘");
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.badgeName()).isEqualTo("새싹");
    }

    @Test
    void headerInfoFallsBackToLoggedOutWhenUserWithdrawn() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        user.withdraw(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserHeaderResponse response = userService.getHeaderInfo(1L);

        assertThat(response.isLoggedIn()).isFalse();
    }

    @Test
    void headerInfoFallsBackToLoggedOutWhenUserHidden() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        user.hideByAdmin();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserHeaderResponse response = userService.getHeaderInfo(1L);

        assertThat(user.isHidden()).isTrue();
        assertThat(response.isLoggedIn()).isFalse();
    }

    @Test
    void withdrawPurgesPersonalDataAndRevokesRefreshTokenSessions() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserWithdrawResponse response = userService.withdraw(
                1L,
                new WithdrawUserRequest("더 이상 서비스를 이용하지 않습니다.")
        );

        assertThat(response.success()).isTrue();
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        // 탈퇴 사유(자유 입력)는 개인정보 보호를 위해 저장하지 않는다.
        assertThat(user.getWithdrawalReason()).isNull();
        // 개인정보 파기와 세션 폐기가 수행된다.
        then(refreshTokenSessionRepository).should().deleteByUserId(1L);
        then(authLoginCodeRepository).should().deleteByUserId(1L);
    }

    @Test
    void agreeTermsCompletesWhenOnlyRequiredTermsAgreedAndAiOmitted() {
        // AI 챗봇 동의는 선택 항목이므로, 미전송(null)이어도 필수 약관만 동의하면 온보딩이 완료된다.
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserAgreementResponse response = userService.agreeTerms(
                1L,
                new CreateUserAgreementRequest(true, true, null)
        );

        assertThat(response.serviceTermsAgreed()).isTrue();
        assertThat(response.privacyPolicyAgreed()).isTrue();
        assertThat(response.aiTermsAgreed()).isFalse();
        assertThat(response.aiTermsAgreedAt()).isNull();
        assertThat(user.getAiTermsAgreedAt()).isNull();
        assertThat(user.isAgreementCompleted()).isTrue();
    }

    @Test
    void agreeTermsStoresAiConsentWhenAgreed() {
        // AI 챗봇 동의를 true로 보내면 그대로 저장된다.
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserAgreementResponse response = userService.agreeTerms(
                1L,
                new CreateUserAgreementRequest(true, true, true)
        );

        assertThat(response.aiTermsAgreed()).isTrue();
        assertThat(response.aiTermsAgreedAt()).isNotNull();
        assertThat(user.getAiTermsAgreedAt()).isEqualTo(response.aiTermsAgreedAt());
    }

    @Test
    void agreeTermsKeepsFirstAiConsentTimeWhenAlreadyAgreed() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        user.agreeTerms(true, true, true);
        var firstAiTermsAgreedAt = user.getAiTermsAgreedAt();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserAgreementResponse response = userService.agreeTerms(
                1L,
                new CreateUserAgreementRequest(true, true, true)
        );

        assertThat(response.aiTermsAgreedAt()).isEqualTo(firstAiTermsAgreedAt);
    }

    @Test
    void agreeTermsRejectsWhenRequiredTermsNotAgreed() {
        // 필수 약관(서비스/개인정보) 미동의 시에는 AI 동의 여부와 무관하게 거부된다.
        assertThatThrownBy(() -> userService.agreeTerms(
                1L,
                new CreateUserAgreementRequest(true, false, true)
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.REQUIRED_TERMS_NOT_AGREED);
    }

    @Test
    void withdrawRejectsAlreadyWithdrawnUser() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        user.withdraw(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.withdraw(1L, null))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ALREADY_WITHDRAWN);
    }

    @Test
    void withdrawAnonymizesPersonalDataAndReleasesSocialIdentity() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        ReflectionTestUtils.setField(user, "id", 1L);

        user.withdraw("탈퇴 사유");

        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getProviderUserId())
                .isNotEqualTo("kakao-1")
                .startsWith("withdrawn:");
        assertThat(user.getAuthSubject()).hasSize(36);
    }

    @Test
    void socialLoginCreatesFreshUserAfterWithdrawal() {
        // 탈퇴 회원은 소셜 식별자가 해제(묘비값)되어 원래 providerUserId로 조회되지 않으므로 신규 가입된다.
        given(userRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> {
            User created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", 2L);
            return created;
        });

        UserService.UserCreationResult result = userService.getOrCreateSocialUser(
                SocialProvider.KAKAO,
                "kakao-1",
                "parent@example.com",
                "민준맘"
        );

        assertThat(result.created()).isTrue();
        assertThat(result.userId()).isEqualTo(2L);
    }

    @Test
    void socialLoginCreatesFreshUserFromLegacyWithdrawnAccount() {
        // 기존 방식(소프트 삭제)으로 탈퇴해 소셜 식별자가 그대로 남아있는 레거시 회원.
        User legacy = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "old@example.com", "옛닉네임");
        ReflectionTestUtils.setField(legacy, "id", 1L);
        ReflectionTestUtils.setField(legacy, "status", UserStatus.DELETED);
        given(userRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.of(legacy));
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", 2L);
            }
            return saved;
        });

        UserService.UserCreationResult result = userService.getOrCreateSocialUser(
                SocialProvider.KAKAO, "kakao-1", "new@example.com", "새닉네임");

        // 레거시 회원의 소셜 식별자가 해제되고, 같은 소셜 계정으로 새 회원이 생성된다.
        assertThat(legacy.getProviderUserId()).startsWith("withdrawn:");
        assertThat(result.created()).isTrue();
        assertThat(result.userId()).isEqualTo(2L);
    }

    @Test
    void socialLoginRejectsHiddenUser() {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        user.hideByAdmin();
        given(userRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getOrCreateSocialUser(
                SocialProvider.KAKAO,
                "kakao-1",
                "parent@example.com",
                "민준맘"
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INACTIVE_USER);
    }
}
