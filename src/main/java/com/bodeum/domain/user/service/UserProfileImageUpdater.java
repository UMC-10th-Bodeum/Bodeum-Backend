package com.bodeum.domain.user.service;

import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.repository.UserAccountRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileImageUpdater {

    private final UserAccountRepository userAccountRepository;

    @Transactional
    public UserProfileResponse updateProfileImage(Long userId, String imageUrl) {
        if (userId == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
        if (!userAccount.isActive()) {
            throw new ProjectException(AuthErrorCode.INACTIVE_USER);
        }

        userAccount.updateProfileImage(imageUrl);

        return UserProfileResponse.from(userAccount);
    }
}
