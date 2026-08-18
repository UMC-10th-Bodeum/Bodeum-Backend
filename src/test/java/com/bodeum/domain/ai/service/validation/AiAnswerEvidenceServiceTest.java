package com.bodeum.domain.ai.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiAnswerEvidenceServiceTest {

    private final AiAnswerEvidenceService evidenceService =
            new AiAnswerEvidenceService(mock(AiSourceReviewRepository.class));

    @Test
    void identifiesServiceDescriptionAsTheSameInstitution() {
        Set<String> previousKeys = evidenceService.sourceIdentityKeys(
                "호매실아동발달심리센터", null);
        Set<String> additionalKeys = evidenceService.sourceIdentityKeys(
                "호매실아동발달심리센터 발달+언어 지원 서비스", null);

        assertThat(previousKeys).contains("institution:호매실아동발달심리센터");
        assertThat(additionalKeys).contains("institution:호매실아동발달심리센터");
    }

    @Test
    void keepsDifferentBranchesAsDifferentInstitutions() {
        Set<String> mainBranchKeys = evidenceService.sourceIdentityKeys(
                "사회적협동조합홀더맘심리언어발달센터(본점)", null);
        Set<String> homaesilBranchKeys = evidenceService.sourceIdentityKeys(
                "사회적협동조합홀더맘심리언어발달센터(호매실점)", null);

        assertThat(mainBranchKeys).doesNotContainAnyElementsOf(homaesilBranchKeys);
    }
}
