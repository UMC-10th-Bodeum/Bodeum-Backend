package com.bodeum.global.auth;

import com.bodeum.domain.auth.enums.SocialProvider;

public record AuthUserPrincipal(
        Long userId,
        SocialProvider provider,
        String nickname,
        String email
) {
}
