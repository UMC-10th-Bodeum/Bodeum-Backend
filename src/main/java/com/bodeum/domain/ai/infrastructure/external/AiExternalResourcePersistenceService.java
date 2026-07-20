package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.entity.AiExternalResource;
import com.bodeum.domain.ai.repository.AiExternalResourceRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiExternalResourcePersistenceService {

    private final AiExternalResourceRepository externalResourceRepository;

    @Transactional
    public List<AiExternalResource> saveAll(Collection<AiExternalResourceCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<String, AiExternalResource> existingByHash = externalResourceRepository
                .findAllBySourceUrlHashIn(candidates.stream()
                        .map(AiExternalResourceCandidate::urlHash)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        AiExternalResource::getSourceUrlHash,
                        resource -> resource
                ));

        List<AiExternalResource> resources = candidates.stream()
                .map(candidate -> updateOrCreate(existingByHash, candidate))
                .toList();
        return externalResourceRepository.saveAll(resources);
    }

    private AiExternalResource updateOrCreate(
            Map<String, AiExternalResource> existingByHash,
            AiExternalResourceCandidate candidate
    ) {
        AiExternalResource existing = existingByHash.get(candidate.urlHash());
        if (existing != null) {
            existing.updateReference(
                    candidate.title(),
                    candidate.normalizedUrl(),
                    existing.getSourceUpdatedAt()
            );
            return existing;
        }
        return AiExternalResource.create(
                candidate.externalSource(),
                candidate.title(),
                candidate.normalizedUrl(),
                candidate.urlHash(),
                null
        );
    }
}
