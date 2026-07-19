package com.bodeum.domain.user.service;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.dto.request.AiTermsAgreementRequest;
import com.bodeum.domain.user.dto.response.AiTermsAgreementResponse;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.service.RegionService;
import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.dto.request.WithdrawUserRequest;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserHeaderResponse;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.dto.response.UserProfileUpdateResponse;
import com.bodeum.domain.user.dto.response.UserWithdrawResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.repository.UserAgreementRepository;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final S3ImageStorage s3ImageStorage;
    private final UserProfileImageUpdater userProfileImageUpdater;
    private final RegionService regionService;
    private final UserAgreementRepository userAgreementRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(getCurrentUser(userId));
    }

    /**
     * 헤더/사이드바 공통 조회. 인증이 없거나 토큰이 만료된 경우에도 예외 없이
     * 비로그인 응답으로 fallback 한다.
     */
    @Transactional(readOnly = true)
    public UserHeaderResponse getHeaderInfo(Long userId) {
        if (userId == null) {
            return UserHeaderResponse.loggedOut();
        }

        return findActiveUser(userId)
                .map(UserHeaderResponse::from)
                .orElseGet(UserHeaderResponse::loggedOut);
    }

    @Transactional
    public UserProfileUpdateResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getCurrentUser(userId);
        Region region = request.regionId() == null ? null : regionService.getById(request.regionId());
        user.updateProfile(
                request.nickname(),
                request.childNickname(),
                request.childBirth(),
                request.disabilityTypes(),
                request.keywordText(),
                request.interestCategories(),
                region,
                request.guardianType(),
                request.communityRoleType()
        );

        return UserProfileUpdateResponse.ofSuccess();
    }

    public UserProfileResponse uploadProfileImage(Long userId, MultipartFile image) {
        getCurrentUser(userId);
        String imageUrl = s3ImageStorage.upload(image, PROFILE_IMAGE_DIRECTORY);

        return userProfileImageUpdater.updateProfileImage(userId, imageUrl);
    }

    @Transactional
    public UserAgreementResponse agreeTerms(Long userId, CreateUserAgreementRequest request) {
        if (!request.hasAgreedRequiredTerms()) {
            throw new ProjectException(AuthErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        User user = getCurrentUser(userId);
        user.agreeTerms(
                request.serviceTermsAgreed(),
                request.privacyPolicyAgreed(),
                request.isAiTermsAgreedValue()
        );

        return UserAgreementResponse.from(user);
    }

    @Transactional
    public UserWithdrawResponse withdraw(Long userId, WithdrawUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
        if (user.isWithdrawn()) {
            throw new ProjectException(AuthErrorCode.ALREADY_WITHDRAWN);
        }

        user.withdraw(request == null ? null : request.reason());
        refreshTokenSessionRepository.deleteByUserId(userId);

        return UserWithdrawResponse.ofSuccess();
    }

    public User getCurrentUser(Long userId) {
        if (userId == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        return getUserById(userId);
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
        Optional<User> existingUser = userRepository.findByProviderAndProviderUserId(
                provider,
                providerUserId
        );
        if (existingUser.isPresent()) {
            return restoreOrRequireActive(existingUser.get());
        }

        try {
            User user = User.createSocialUser(provider, providerUserId, email, nickname);
            return new UserCreationResult(userRepository.saveAndFlush(user).getId(), true);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                    .map(this::restoreOrRequireActive)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
        return requireActive(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive);
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveUserByAuthSubject(String authSubject) {
        return userRepository.findByAuthSubject(authSubject)
                .filter(User::isActive);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByAuthSubject(String authSubject) {
        return userRepository.findByAuthSubject(authSubject);
    }

    @Transactional(readOnly = true)
    public AiTermsAgreementResponse getAiTermsAgreement(Long userId) {
        return userAgreementRepository.findByUserId(userId)
                .map(a -> AiTermsAgreementResponse.of(a.isAiTermsAgreed(), a.getAiTermsAgreedAt()))
                .orElse(AiTermsAgreementResponse.of(false, null));
    }

    @Transactional
    public AiTermsAgreementResponse agreeAiTerms(
            Long userId,
            AiTermsAgreementRequest request
    ) {
        UserAgreement agreement = userAgreementRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = getCurrentUser(userId);
                    return userAgreementRepository.save(UserAgreement.create(user, false, false, false));
                });

        agreement.agreeAiTerms();

        return AiTermsAgreementResponse.of(
                agreement.isAiTermsAgreed(),
                agreement.getAiTermsAgreedAt()
        );
    }

    private User requireActive(User user) {
        if (!user.isActive()) {
            throw new ProjectException(AuthErrorCode.INACTIVE_USER);
        }

        return user;
    }

    private UserCreationResult restoreOrRequireActive(User user) {
        if (user.isWithdrawn()) {
            user.reactivate();
            return new UserCreationResult(userRepository.saveAndFlush(user).getId(), false);
        }

        return new UserCreationResult(requireActive(user).getId(), false);
    }

    public record UserCreationResult(
            Long userId,
            boolean created
    ) {
    }
}
