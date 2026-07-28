package com.bodeum.domain.user.repository;

import com.bodeum.domain.user.entity.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {

    Optional<UserAgreement> findByUserId(Long userId);
}
