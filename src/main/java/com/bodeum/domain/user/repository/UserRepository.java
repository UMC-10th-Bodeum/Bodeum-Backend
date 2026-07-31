package com.bodeum.domain.user.repository;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    Optional<User> findByAuthSubject(String authSubject);

    // 보존 콘텐츠(게시글·댓글)의 작성자 익명화용. 주어진 id 중 탈퇴(DELETED) 상태인 회원 id만 반환한다.
    @Query("""
            SELECT u.id FROM User u
            WHERE u.id IN :ids
              AND u.status = com.bodeum.domain.user.enums.UserStatus.DELETED
            """)
    List<Long> findWithdrawnUserIdsByIdIn(@Param("ids") Collection<Long> ids);

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

    @Query("select u from User u " +
            "left join fetch u.guardianProfile gp " +
            "left join fetch gp.region " +
            "where u.id = :id")
    Optional<User> findByIdWithGuardianProfileAndRegion(@Param("id") Long id);
}
