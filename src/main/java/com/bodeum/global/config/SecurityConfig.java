package com.bodeum.global.config;

import com.bodeum.global.auth.ApiAccessDeniedHandler;
import com.bodeum.global.auth.ApiAuthenticationEntryPoint;
import com.bodeum.global.auth.BearerTokenAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final CorsProperties corsProperties;

    // HTTP Method 상관없이 모두 허용할 URI (로그인, Swagger, 공통 정적 URI 등)
    private final String[] allowUris = {
            // 헬스 체크(Docker HEALTHCHECK, 배포 검증, 외부 모니터링에서 인증 없이 호출한다)
            "/actuator/health",
            "/actuator/health/**",
            // Swagger 허용
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            // Auth API
            "/api/v1/auth/login/**",
            "/api/v1/auth/callback/**",
            "/api/v1/auth/exchange",
            "/api/v1/auth/refresh",
            "/api/v1/terms/**",
            // 공통 및 홈 화면 API
            "/api/v1/users/me/brief",
            "/api/v1/news/recommended",
            "/api/v1/home/posts/preview",
            "/api/v1/community/posts/recommended",
            "/api/v1/home/news/preview",
            "/api/v1/info-items/counts",
            "/api/v1/home/banner",
            // 검색 및 지역 API
            "/api/v1/search",
            // 검색 API
            "/api/v1/info/search",

            // Open API 동기화
            "/api/v1/admin/openapi/**",

            // 정보 항목 조회 및 지역 필터 API (비회원 허용)
            "/api/v1/info-items/**",
            "/api/v1/info-regions/**",
            // 동기화 API
            "/api/v1/admin/sync/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // 1. 공통 허용 URI
                        .requestMatchers(allowUris).permitAll()

                        // 2. 정보(Info) 관련 비로그인 허용 API (GET 조회 전체 + 카카오지도 생성 POST)
                        .requestMatchers(HttpMethod.GET, "/api/v1/info-items/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/info-items/kakaomap-url").permitAll()

                        // 3. 뉴스 및 커뮤니티 GET 조회 허용 API
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/news",
                                "/api/v1/news/*",
                                "/api/v1/news/*/related",
                                "/api/v1/news/search/suggestions",
                                "/api/community/posts/*/comments"
                        ).permitAll()

                        // 4. 그 외 스크랩, 리뷰 작성/공감 등 CUD 요청은 인증(로그인) 필수
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts/*/comments").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler)
                )
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        // JWT 헤더 전송 방식이며 필요에 따라 true/false 세팅
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
