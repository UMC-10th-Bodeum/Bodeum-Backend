package com.bodeum.global.config;

import com.bodeum.global.auth.LoginUserArgumentResolver;
import com.bodeum.global.auth.SignupCompletedInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final SignupCompletedInterceptor signupCompletedInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // @RequireSignupCompleted가 붙은 핸들러에서만 동작하므로 경로 제한 없이 전역 등록한다.
        registry.addInterceptor(signupCompletedInterceptor);
    }
}
