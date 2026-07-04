package com.bodeum.domain.user.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class UserAccountStore {

    private final AtomicLong userIdSequence = new AtomicLong(1L);
    private final ConcurrentMap<Long, UserAccount> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> providerUserIndex = new ConcurrentHashMap<>();

    public UserCreationResult getOrCreateSocialUser(
            SocialProvider provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        String indexKey = createIndexKey(provider, providerUserId);
        AtomicBoolean created = new AtomicBoolean(false);

        Long userId = providerUserIndex.computeIfAbsent(indexKey, key -> {
            Long newUserId = userIdSequence.getAndIncrement();
            users.put(newUserId, UserAccount.createSocialUser(newUserId, provider, providerUserId, email, nickname));
            created.set(true);
            return newUserId;
        });

        return new UserCreationResult(getUserById(userId), created.get());
    }

    public UserAccount getUserById(Long userId) {
        return findActiveUser(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
    }

    public Optional<UserAccount> findActiveUser(Long userId) {
        UserAccount userAccount = users.get(userId);
        if (userAccount == null || userAccount.isWithdrawn()) {
            return Optional.empty();
        }

        return Optional.of(userAccount);
    }

    private String createIndexKey(SocialProvider provider, String providerUserId) {
        return provider.getPath() + ":" + providerUserId;
    }

    public record UserCreationResult(
            UserAccount userAccount,
            boolean created
    ) {
    }
}
