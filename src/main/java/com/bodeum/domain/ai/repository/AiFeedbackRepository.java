package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    boolean existsByAiMessageId(Long aiMessageId);
}
