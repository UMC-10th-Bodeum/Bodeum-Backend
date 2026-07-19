package com.bodeum.domain.user.repository;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    Optional<User> findByAuthSubject(String authSubject);

    @EntityGraph(attributePaths = {
            "childProfile", "childProfile.disabilities", "guardianProfile",
            "guardianProfile.region", "userInterests"
    })
    Optional<User> findAiProfileById(Long id);
}
