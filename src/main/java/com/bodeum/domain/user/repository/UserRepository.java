package com.bodeum.domain.user.repository;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.UserStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    Optional<User> findByAuthSubject(String authSubject);

    // 공개 콘텐츠(글·댓글) 작성자 익명화용: 주어진 id 중 탈퇴(DELETED) 상태인 사용자 id만 조회한다.
    @Query("select u.id from User u where u.id in :ids and u.status = :status")
    Set<Long> findIdsByIdInAndStatus(@Param("ids") Collection<Long> ids, @Param("status") UserStatus status);

    @EntityGraph(attributePaths = {
            "childProfile",
            "guardianProfile",
            "guardianProfile.region",
            "userInterests"
    })
    Optional<User> findAiProfileById(Long id);

    @EntityGraph(attributePaths = {
            "childProfile",
            "childProfile.disabilities"
    })
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findAiDisabilityProfileById(@Param("id") Long id);
}
