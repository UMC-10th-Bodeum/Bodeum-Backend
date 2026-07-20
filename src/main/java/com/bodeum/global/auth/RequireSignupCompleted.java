package com.bodeum.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 가입이 확정된(온보딩 완료 또는 건너뛰기) 정식 회원만 접근을 허용한다.
 *
 * <p>메서드 또는 컨트롤러 클래스에 붙이며, 가입 미완료(registeredAt == null) 사용자의
 * 요청은 {@code SignupCompletedInterceptor}가 403(AUTH403_1)으로 차단한다.
 *
 * <p>opt-in 방식이라 이 어노테이션이 없는 엔드포인트의 동작에는 전혀 영향을 주지 않는다.
 * 각 도메인(home/info/ai-chatbot 등)이 정식 회원만 허용할 기능에 선택적으로 붙인다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSignupCompleted {
}
