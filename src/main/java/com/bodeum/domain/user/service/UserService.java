package com.bodeum.domain.user.service;

import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.dto.response.UserSummaryResponse;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountStore userAccountStore;

    @Transactional(readOnly = true)
    public UserSummaryResponse getSummary(Authentication authentication) {
        return UserSummaryResponse.from(getCurrentUser(authentication));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Authentication authentication) {
        return UserProfileResponse.from(getCurrentUser(authentication));
    }

    @Transactional
    public UserProfileResponse updateProfile(Authentication authentication, UpdateUserProfileRequest request) {
        UserAccount userAccount = getCurrentUser(authentication);
        userAccount.updateProfile(request.nickname(), request.childName(), request.guardianType());

        return UserProfileResponse.from(userAccount);
    }

    @Transactional
    public UserAgreementResponse agreeTerms(Authentication authentication, CreateUserAgreementRequest request) {
        UserAccount userAccount = getCurrentUser(authentication);
        userAccount.agreeTerms(
                request.serviceTermsAgreed(),
                request.privacyPolicyAgreed(),
                request.isAiChatAgreedValue()
        );

        return UserAgreementResponse.from(userAccount);
    }

    @Transactional
    public void withdraw(Authentication authentication) {
        getCurrentUser(authentication).withdraw();
    }

    public UserAccount getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        return userAccountStore.getUserById(principal.userId());
    }
}
