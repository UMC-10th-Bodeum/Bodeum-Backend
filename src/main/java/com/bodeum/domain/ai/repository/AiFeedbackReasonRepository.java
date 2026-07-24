package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiFeedbackReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackReasonRepository extends JpaRepository<AiFeedbackReason, Long> {
}
