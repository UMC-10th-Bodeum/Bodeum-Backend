package com.bodeum.domain.home.repository;

import com.bodeum.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HomeUserRepository extends JpaRepository<User, Long> {

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.guardianProfile gp
            LEFT JOIN FETCH gp.region
            WHERE u.id = :userId
            """)
    Optional<User> findWithRegionById(@Param("userId") Long userId);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.guardianProfile gp
            LEFT JOIN FETCH gp.region
            LEFT JOIN FETCH u.userInterests
            WHERE u.id = :userId
            """)
    Optional<User> findWithRegionAndInterestsById(@Param("userId") Long userId);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.childProfile cp
            LEFT JOIN FETCH cp.disabilities
            WHERE u.id = :userId
            """)
    Optional<User> findWithChildDisabilitiesById(@Param("userId") Long userId);
}
