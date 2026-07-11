package com.bodeum.domain.user.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.onboarding.enumtype.CareArea;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.InterestCategory;
import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserHeaderResponse;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.dto.response.UserSummaryResponse;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.repository.UserAccountRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";

    private final UserAccountRepository userAccountRepository;
    private final S3ImageStorage s3ImageStorage;

    @Transactional(readOnly = true)
    public UserSummaryResponse getSummary(Authentication authentication) {
        return UserSummaryResponse.from(getCurrentUser(authentication));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Authentication authentication) {
        return UserProfileResponse.from(getCurrentUser(authentication));
    }

    /**
     * 헤더/사이드바 공통 조회. 인증이 없거나 토큰이 만료된 경우에도 예외 없이
     * 비로그인 응답으로 fallback 한다.
     */
    @Transactional(readOnly = true)
    public UserHeaderResponse getHeaderInfo(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return UserHeaderResponse.loggedOut();
        }

        return findActiveUser(principal.userId())
                .map(UserHeaderResponse::from)
                .orElseGet(UserHeaderResponse::loggedOut);
    }

    @Transactional
    public UserProfileResponse updateProfile(Authentication authentication, UpdateUserProfileRequest request) {
        UserAccount userAccount = getCurrentUser(authentication);
        userAccount.updateProfile(
                request.nickname(),
                request.childNickname(),
                request.childBirthYear(),
                request.childBirthMonth(),
                toCareAreas(request.careAreas()),
                request.characteristicKeyword(),
                toInterests(request.interests()),
                request.regionLevel1(),
                request.regionLevel2(),
                GuardianType.fromNullable(request.guardianType()),
                CommunityRoleType.fromNullable(request.communityRoleType())
        );

        return UserProfileResponse.from(userAccount);
    }

    @Transactional
    public UserProfileResponse uploadProfileImage(Authentication authentication, MultipartFile image) {
        UserAccount userAccount = getCurrentUser(authentication);
        String imageUrl = s3ImageStorage.upload(image, PROFILE_IMAGE_DIRECTORY);
        userAccount.updateProfileImage(imageUrl);

        return UserProfileResponse.from(userAccount);
    }

    private List<CareArea> toCareAreas(List<String> careAreas) {
        return careAreas == null ? null : careAreas.stream().map(CareArea::from).toList();
    }

    private List<InterestCategory> toInterests(List<String> interests) {
        return interests == null ? null : interests.stream().map(InterestCategory::from).toList();
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

        return getUserById(principal.userId());
    }

    /**
     * 동시 첫 로그인 복구를 위해 호출자의 트랜잭션에 참여하지 않는다.
     * 이렇게 하면 (1) 호출자가 연 트랜잭션을 unique 제약 위반이 rollback-only로
     * 오염시키지 않고, (2) 각 repository 호출이 독립 트랜잭션(독립 스냅샷)을 가져
     * DataIntegrityViolationException 이후에도 재조회로 안전하게 복구된다.
     *
     * 엔티티가 아니라 식별자만 반환한다. 반환 엔티티는 이 메서드 종료 시점에 detach 되어
     * LAZY 필드를 읽을 수 없으므로, 호출자가 자신의 트랜잭션에서 userId로 다시 조회한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserCreationResult getOrCreateSocialUser(
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        Optional<UserAccount> existingUser = userAccountRepository.findByProviderAndProviderUserId(
                provider,
                providerUserId
        );
        if (existingUser.isPresent()) {
            return new UserCreationResult(requireActive(existingUser.get()).getId(), false);
        }

        try {
            UserAccount userAccount = UserAccount.createSocialUser(provider, providerUserId, email, nickname);
            return new UserCreationResult(userAccountRepository.saveAndFlush(userAccount).getId(), true);
        } catch (DataIntegrityViolationException e) {
            return userAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                    .map(userAccount -> new UserCreationResult(requireActive(userAccount).getId(), false))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public UserAccount getUserById(Long userId) {
        return findActiveUser(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findActiveUser(Long userId) {
        return userAccountRepository.findById(userId)
                .filter(userAccount -> !userAccount.isWithdrawn());
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findActiveUserByAuthSubject(String authSubject) {
        return userAccountRepository.findByAuthSubject(authSubject)
                .filter(userAccount -> !userAccount.isWithdrawn());
    }

    private UserAccount requireActive(UserAccount userAccount) {
        if (userAccount.isWithdrawn()) {
            throw new ProjectException(AuthErrorCode.INACTIVE_USER);
        }

        return userAccount;
    }

    public record UserCreationResult(
            Long userId,
            boolean created
    ) {
    }
}
