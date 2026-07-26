package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InfoScrapRepository extends JpaRepository<InfoScrap, Long> {

    Optional<InfoScrap> findByUserAndInfoItem(User user, InfoItem infoItem);

    boolean existsByUserAndInfoItem(User user, InfoItem infoItem);
}