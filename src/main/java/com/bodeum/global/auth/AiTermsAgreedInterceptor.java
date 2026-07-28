package com.bodeum.global.auth;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserAgreementRepository;
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
 * {@link RequireAiTermsAgreed}가 붙은 핸들러의 AI 챗봇 이용동의 여부를 검사한다.
 */
@Component
@RequiredArgsConstructor
public class AiTermsAgreedInterceptor implements HandlerInterceptor {

    private final UserAgreementRepository userAgreementRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod) || !requiresAiTermsAgreed(handlerMethod)) {
            return true;
        }

        Long userId = resolveUserId();
        if (userId == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        UserAgreement agreement = userAgreementRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_AGREEMENT_NOT_FOUND));
        if (!agreement.isAiTermsAgreed()) {
            throw new ProjectException(AiErrorCode.AI_TERMS_NOT_AGREED);
        }

        return true;
    }

    private boolean requiresAiTermsAgreed(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireAiTermsAgreed.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAiTermsAgreed.class);
    }

    private Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return null;
        }

        return principal.userId();
    }
}
