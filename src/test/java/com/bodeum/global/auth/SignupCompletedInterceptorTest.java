package com.bodeum.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.lang.reflect.Method;
import java.util.List;
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
class SignupCompletedInterceptorTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private SignupCompletedInterceptor interceptor;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsHandlerWithoutAnnotationRegardlessOfSignupStatus() throws Exception {
        // 어노테이션이 없는 핸들러는 가입 완료 여부와 무관하게 그대로 통과한다(opt-in).
        boolean result = interceptor.preHandle(request, response, handlerMethod("plain"));

        assertThat(result).isTrue();
    }

    @Test
    void allowsSignupCompletedUserOnGuardedHandler() throws Exception {
        authenticate(1L);
        given(userService.getUserById(1L)).willReturn(signupCompletedUser());

        boolean result = interceptor.preHandle(request, response, handlerMethod("guarded"));

        assertThat(result).isTrue();
    }

    @Test
    void blocksSignupIncompleteUserOnGuardedHandler() throws Exception {
        authenticate(1L);
        given(userService.getUserById(1L)).willReturn(signupIncompleteUser());

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod("guarded")))
                .isInstanceOf(ProjectException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.SIGNUP_NOT_COMPLETED);
    }

    @Test
    void blocksWhenPrincipalMissingOnGuardedHandler() throws Exception {
        // 인증 주체가 없으면 인가 이전 문제이므로 401로 막는다.
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod("guarded")))
                .isInstanceOf(ProjectException.class)
                .extracting("errorCode")
                .isEqualTo(GeneralErrorCode.UNAUTHORIZED);
    }

    private void authenticate(Long userId) {
        AuthUserPrincipal principal = new AuthUserPrincipal(userId, SocialProvider.KAKAO, "민준맘", "parent@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private User signupCompletedUser() {
        User user = newUser();
        user.skipOnboarding();
        user.markRegisteredIfResolved();
        return user;
    }

    private User signupIncompleteUser() {
        return newUser();
    }

    private User newUser() {
        return User.createSocialUser(SocialProvider.KAKAO, "kakao-user-1", "parent@example.com", "민준맘");
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    static class TestController {

        @RequireSignupCompleted
        public void guarded() {
        }

        public void plain() {
        }
    }
}
