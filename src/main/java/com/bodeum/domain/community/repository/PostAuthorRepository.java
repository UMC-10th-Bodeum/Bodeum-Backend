package com.bodeum.domain.community.repository;

import com.bodeum.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface PostAuthorRepository extends Repository<User, Long> {

    @EntityGraph(attributePaths = "guardianProfile")
    List<User> findAllByIdIn(Collection<Long> ids);
}
