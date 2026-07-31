package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InfoTagRepository extends JpaRepository<InfoTag, Long> {
    Optional<InfoTag> findByName(String name);
}