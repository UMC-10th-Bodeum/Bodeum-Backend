package com.bodeum.domain.auth.service;

public record SocialUserProfile(
        String providerUserId,
        String email,
        String nickname
) {
}
