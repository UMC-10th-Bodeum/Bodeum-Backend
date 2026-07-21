package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.entity.AiExternalDocument;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiExternalDocumentRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
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
public class AiExternalDocumentPersistenceService {

    private static final String UPSERT_SQL = """
            INSERT INTO ai_external_document (
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

    private final AiExternalDocumentRepository externalDocumentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public List<AiExternalDocument> saveAll(Collection<AiExternalDocumentCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        // UNIQUE URL hash와 MySQL upsert를 함께 사용해 동시 요청에서도
        // 같은 외부 문서가 중복 INSERT되어 한 요청이 실패하는 경쟁 조건을 막는다.
        jdbcTemplate.batchUpdate(UPSERT_SQL, candidates.stream()
                .map(candidate -> new Object[]{
                        candidate.externalSource().getId(),
                        candidate.title(),
                        candidate.normalizedUrl(),
                        candidate.urlHash()
                })
                .toList());

        Map<String, AiExternalDocument> documentsByHash = externalDocumentRepository
                .findAllBySourceUrlHashIn(candidates.stream()
                        .map(AiExternalDocumentCandidate::urlHash)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        AiExternalDocument::getSourceUrlHash,
                        document -> document
                ));

        return candidates.stream()
                .map(candidate -> findSavedDocument(documentsByHash, candidate.urlHash()))
                .toList();
    }

    private AiExternalDocument findSavedDocument(
            Map<String, AiExternalDocument> documentsByHash,
            String urlHash
    ) {
        AiExternalDocument document = documentsByHash.get(urlHash);
        if (document == null) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED);
        }
        return document;
    }
}
