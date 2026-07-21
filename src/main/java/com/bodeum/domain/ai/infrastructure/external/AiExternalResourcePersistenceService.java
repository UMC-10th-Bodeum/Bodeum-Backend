package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.entity.AiExternalResource;
import com.bodeum.domain.ai.repository.AiExternalResourceRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiExternalResourcePersistenceService {

    private static final String UPSERT_SQL = """
            INSERT INTO ai_external_resource (
                ai_external_source_id,
                title,
                source_url,
                source_url_hash,
                source_updated_at,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, NULL, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE
                updated_at = IF(
                    ai_external_source_id <> VALUES(ai_external_source_id)
                        OR title <> VALUES(title)
                        OR source_url <> VALUES(source_url),
                    NOW(6),
                    updated_at
                ),
                ai_external_source_id = VALUES(ai_external_source_id),
                title = VALUES(title),
                source_url = VALUES(source_url)
            """;

    private final AiExternalResourceRepository externalResourceRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public List<AiExternalResource> saveAll(Collection<AiExternalResourceCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, candidates.stream()
                .map(candidate -> new Object[]{
                        candidate.externalSource().getId(),
                        candidate.title(),
                        candidate.normalizedUrl(),
                        candidate.urlHash()
                })
                .toList());

        Map<String, AiExternalResource> resourcesByHash = externalResourceRepository
                .findAllBySourceUrlHashIn(candidates.stream()
                        .map(AiExternalResourceCandidate::urlHash)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        AiExternalResource::getSourceUrlHash,
                        resource -> resource
                ));

        return candidates.stream()
                .map(candidate -> findSavedResource(resourcesByHash, candidate.urlHash()))
                .toList();
    }

    private AiExternalResource findSavedResource(
            Map<String, AiExternalResource> resourcesByHash,
            String urlHash
    ) {
        AiExternalResource resource = resourcesByHash.get(urlHash);
        if (resource == null) {
            throw new IllegalStateException(
                    "Upserted external resource could not be found: " + urlHash);
        }
        return resource;
    }
}
