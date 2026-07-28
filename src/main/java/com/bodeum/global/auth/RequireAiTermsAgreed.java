package com.bodeum.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 챗봇 이용동의를 완료한 사용자만 접근을 허용한다.
 *
 * <p>가입 완료 검증이 함께 필요한 API는 {@link RequireSignupCompleted}를 별도로 적용한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAiTermsAgreed {
}
