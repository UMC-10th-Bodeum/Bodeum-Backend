package com.bodeum.domain.auth.controller;

import com.bodeum.domain.auth.dto.request.LogoutAuthRequest;
import com.bodeum.domain.auth.dto.request.RefreshAuthTokenRequest;
import com.bodeum.domain.auth.dto.response.AuthLoginResponse;
import com.bodeum.domain.auth.dto.response.AuthTokenResponse;
import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.auth.service.AuthService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> redirectSocialLogin(
            @PathVariable String provider
    ) {
        URI redirectUri = authService.createLoginRedirectUri(SocialProvider.from(provider));

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @GetMapping("/callback/{provider}")
    public ApiResponse<AuthLoginResponse> socialLoginCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                authService.loginWithCallback(SocialProvider.from(provider), code, state)
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refreshToken(
            @Valid @RequestBody RefreshAuthTokenRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody LogoutAuthRequest request
    ) {
        authService.logout(request.refreshToken());

        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}
