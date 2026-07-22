package com.bodeum.domain.auth.controller;

import com.bodeum.domain.auth.dto.request.ExchangeAuthCodeRequest;
import com.bodeum.domain.auth.dto.request.LogoutAuthRequest;
import com.bodeum.domain.auth.dto.request.RefreshAuthTokenRequest;
import com.bodeum.domain.auth.dto.response.AuthLoginResponse;
import com.bodeum.domain.auth.dto.response.AuthTokenResponse;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.service.AuthService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "소셜 로그인 및 토큰 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "소셜 로그인 리다이렉트", description = "소셜 인증 페이지로 리다이렉트할 URL(302)을 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "소셜 인증 페이지로 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "AUTH400_1: 지원하지 않는 제공자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "AUTH500_1: 소셜 로그인 설정 미완료"
            )
    })
    @SecurityRequirements
    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> redirectSocialLogin(
            @Parameter(description = "소셜 로그인 제공자", required = true, example = "kakao")
            @PathVariable String provider
    ) {
        URI redirectUri = authService.createLoginRedirectUri(SocialProvider.from(provider));

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @Operation(summary = "소셜 로그인 콜백",
            description = "소셜 인증 후 로그인/회원가입을 처리하고 프론트 콜백 URL로 리다이렉트(302)한다. "
                    + "성공 시 일회용 code(?code=), 실패 시 에러 코드(?error=)를 붙인다. "
                    + "브라우저 전체 네비게이션이라 성공/실패 모두 JSON이 아닌 리다이렉트로 응답한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "성공: 프론트 콜백 URL로 리다이렉트(?code=일회용code) / "
                            + "실패: 프론트 콜백 URL로 리다이렉트(?error=에러코드, 예: AUTH401_6/AUTH400_2/AUTH500_1)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500_1: 예기치 않은 서버 오류(도메인 예외가 아닌 경우에만 JSON 응답)"
            )
    })
    @SecurityRequirements
    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> socialLoginCallback(
            @Parameter(description = "소셜 로그인 제공자", required = true, example = "kakao")
            @PathVariable String provider,
            @Parameter(description = "소셜 제공자가 발급한 인가 코드", required = true)
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state
    ) {
        URI redirectUri;
        try {
            redirectUri = authService.loginWithCallback(SocialProvider.from(provider), code, state);
        } catch (ProjectException e) {
            // 콜백은 브라우저 전체 네비게이션이라 실패도 JSON 대신 프론트로 에러 리다이렉트한다.
            redirectUri = authService.buildFrontErrorRedirectUri(e.getErrorCode());
        }

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @Operation(summary = "일회용 로그인 code 교환",
            description = "콜백이 프론트로 전달한 일회용 code를 토큰/다음 단계로 교환한다. code는 1회용이며 교환 즉시 폐기된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH401_5: 비활성 사용자 / AUTH401_7: 유효하지 않거나 만료된 로그인 code"
            )
    })
    @SecurityRequirements
    @PostMapping("/exchange")
    public ApiResponse<AuthLoginResponse> exchangeLoginCode(
            @Valid @RequestBody ExchangeAuthCodeRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, authService.exchange(request.code()));
    }

    @Operation(summary = "액세스 토큰 재발급", description = "유효한 리프레시 토큰으로 새 액세스/리프레시 토큰 쌍을 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH401_3: 유효하지 않은 refreshToken / AUTH401_5: 비활성 사용자"
            )
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refreshToken(
            @Valid @RequestBody RefreshAuthTokenRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "전달된 리프레시 토큰 세션을 폐기한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @LoginUser Long userId,
            @Valid @RequestBody LogoutAuthRequest request
    ) {
        authService.logout(userId, request.refreshToken());

        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}
