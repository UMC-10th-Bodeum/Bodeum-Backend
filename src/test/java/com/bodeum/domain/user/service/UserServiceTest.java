package com.bodeum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.WithdrawUserRequest;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserHeaderResponse;
import com.bodeum.domain.user.dto.response.UserWithdrawResponse;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.repository.UserAccountRepository;
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
    private UserAccountRepository userAccountRepository;

    @Mock
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Mock
    private S3ImageStorage s3ImageStorage;

    @Mock
    private UserProfileImageUpdater userProfileImageUpdater;

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
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserHeaderResponse response = userService.getHeaderInfo(1L);

        assertThat(response.isLoggedIn()).isTrue();
        assertThat(response.nickname()).isEqualTo("민준맘");
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.badgeName()).isEqualTo("새싹");
    }

    @Test
    void headerInfoFallsBackToLoggedOutWhenUserWithdrawn() {
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        userAccount.withdraw(null);
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserHeaderResponse response = userService.getHeaderInfo(1L);

        assertThat(response.isLoggedIn()).isFalse();
    }

    @Test
    void withdrawStoresReasonAndRevokesRefreshTokenSessions() {
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        ReflectionTestUtils.setField(userAccount, "id", 1L);
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserWithdrawResponse response = userService.withdraw(
                1L,
                new WithdrawUserRequest("더 이상 서비스를 이용하지 않습니다.")
        );

        assertThat(response.success()).isTrue();
        assertThat(userAccount.isWithdrawn()).isTrue();
        assertThat(userAccount.getDeletedAt()).isNotNull();
        assertThat(userAccount.getWithdrawalReason()).isEqualTo("더 이상 서비스를 이용하지 않습니다.");
        then(refreshTokenSessionRepository).should().deleteByUserId(1L);
    }

    @Test
    void agreeTermsCompletesWhenOnlyRequiredTermsAgreedAndAiOmitted() {
        // AI 챗봇 동의는 선택 항목이므로, 미전송(null)이어도 필수 약관만 동의하면 온보딩이 완료된다.
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserAgreementResponse response = userService.agreeTerms(
                1L,
                new CreateUserAgreementRequest(true, true, null)
        );

        assertThat(response.serviceTermsAgreed()).isTrue();
        assertThat(response.privacyPolicyAgreed()).isTrue();
        assertThat(response.aiTermsAgreed()).isFalse();
        assertThat(userAccount.isAgreementCompleted()).isTrue();
    }

    @Test
    void agreeTermsStoresAiConsentWhenAgreed() {
        // AI 챗봇 동의를 true로 보내면 그대로 저장된다.
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserAgreementResponse response = userService.agreeTerms(
                1L,
                new CreateUserAgreementRequest(true, true, true)
        );

        assertThat(response.aiTermsAgreed()).isTrue();
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
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        userAccount.withdraw(null);
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        assertThatThrownBy(() -> userService.withdraw(1L, null))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ALREADY_WITHDRAWN);
    }
}
