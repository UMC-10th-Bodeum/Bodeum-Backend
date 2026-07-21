package com.bodeum.global.auth;

import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link RequireSignupCompleted}가 붙은 핸들러에 한해, 가입이 확정된 정식 회원만 통과시킨다.
 *
 * <p>인증 자체는 상위의 {@code BearerTokenAuthenticationFilter}/SecurityConfig가 담당하고,
 * 이 인터셉터는 그 위에서 "가입 완료 여부(registeredAt)"만 추가로 검사한다.
 * 가입 완료 여부는 토큰 발급 이후에도 바뀔 수 있으므로 매 요청 시 DB의 최신 값을 읽는다.
 */
@Component
@RequiredArgsConstructor
public class SignupCompletedInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod) || !requiresSignupCompleted(handlerMethod)) {
            return true;
        }

        Long userId = resolveUserId();
        if (userId == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        User user = userService.getUserById(userId);
        if (!user.isSignupCompleted()) {
            throw new ProjectException(AuthErrorCode.SIGNUP_NOT_COMPLETED);
        }

        return true;
    }

    private boolean requiresSignupCompleted(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireSignupCompleted.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireSignupCompleted.class);
    }

    private Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return null;
        }

        return principal.userId();
    }
}
