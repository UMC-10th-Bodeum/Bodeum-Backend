package com.bodeum.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserAgreementRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
class AiTermsAgreedInterceptorTest {

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @InjectMocks
    private AiTermsAgreedInterceptor interceptor;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsHandlerWithoutAnnotation() throws Exception {
        boolean result = interceptor.preHandle(request, response, methodHandler("plain"));

        assertThat(result).isTrue();
    }

    @Test
    void allowsUserWhoAgreedToAiTerms() throws Exception {
        authenticate(1L);
        given(userAgreementRepository.findByUserId(1L))
                .willReturn(Optional.of(agreement(true)));

        boolean result = interceptor.preHandle(request, response, methodHandler("guarded"));

        assertThat(result).isTrue();
    }

    @Test
    void appliesGuardDeclaredOnControllerClass() throws Exception {
        authenticate(1L);
        given(userAgreementRepository.findByUserId(1L))
                .willReturn(Optional.of(agreement(true)));

        Method method = ClassGuardedController.class.getMethod("guarded");
        boolean result = interceptor.preHandle(
                request,
                response,
                new HandlerMethod(new ClassGuardedController(), method)
        );

        assertThat(result).isTrue();
    }

    @Test
    void blocksUserWhoDidNotAgreeToAiTerms() throws Exception {
        authenticate(1L);
        given(userAgreementRepository.findByUserId(1L))
                .willReturn(Optional.of(agreement(false)));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, methodHandler("guarded")))
                .isInstanceOf(ProjectException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.AI_TERMS_NOT_AGREED);
    }

    @Test
    void blocksWhenAgreementDoesNotExist() throws Exception {
        authenticate(1L);
        given(userAgreementRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preHandle(request, response, methodHandler("guarded")))
                .isInstanceOf(ProjectException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_AGREEMENT_NOT_FOUND);
    }

    @Test
    void blocksWhenPrincipalIsMissing() throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, methodHandler("guarded")))
                .isInstanceOf(ProjectException.class)
                .extracting("errorCode")
                .isEqualTo(GeneralErrorCode.UNAUTHORIZED);
    }

    private void authenticate(Long userId) {
        AuthUserPrincipal principal = new AuthUserPrincipal(
                userId,
                SocialProvider.KAKAO,
                "민준맘",
                "parent@example.com"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private UserAgreement agreement(boolean aiTermsAgreed) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-user-1",
                "parent@example.com",
                "민준맘"
        );
        return UserAgreement.create(user, true, true, aiTermsAgreed);
    }

    private HandlerMethod methodHandler(String methodName) throws NoSuchMethodException {
        Method method = MethodGuardedController.class.getMethod(methodName);
        return new HandlerMethod(new MethodGuardedController(), method);
    }

    static class MethodGuardedController {

        @RequireAiTermsAgreed
        public void guarded() {
        }

        public void plain() {
        }
    }

    @RequireAiTermsAgreed
    static class ClassGuardedController {

        public void guarded() {
        }
    }
}
