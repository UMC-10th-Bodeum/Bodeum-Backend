package com.bodeum.domain.user.service;

import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileImageUpdater {

    private final UserRepository userRepository;

    @Transactional
    public UserProfileResponse updateProfileImage(Long userId, String imageUrl) {
        if (userId == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ProjectException(AuthErrorCode.INACTIVE_USER);
        }

        user.updateProfileImage(imageUrl);

        return UserProfileResponse.from(user);
    }
}
