package com.bodeum.domain.user.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.domain.user.repository.UserAccountRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserAccountStore {

    private final UserAccountRepository userAccountRepository;

    /**
     * 동시 첫 로그인 복구를 위해 호출자의 트랜잭션에 참여하지 않는다.
     * unique 제약 위반이 발생해도 바깥 트랜잭션을 rollback-only로 만들지 않고,
     * 각 repository 호출의 독립 트랜잭션 안에서 생성 또는 재조회가 끝나도록 한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserCreationResult getOrCreateSocialUser(
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        Optional<UserAccount> existingUser = userAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingUser.isPresent()) {
            return new UserCreationResult(requireActive(existingUser.get()), false);
        }

        try {
            UserAccount userAccount = UserAccount.createSocialUser(provider, providerUserId, email, nickname);
            return new UserCreationResult(userAccountRepository.saveAndFlush(userAccount), true);
        } catch (DataIntegrityViolationException e) {
            return userAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                    .map(userAccount -> new UserCreationResult(requireActive(userAccount), false))
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
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        return userAccount;
    }

    public record UserCreationResult(
            UserAccount userAccount,
            boolean created
    ) {
    }
}
