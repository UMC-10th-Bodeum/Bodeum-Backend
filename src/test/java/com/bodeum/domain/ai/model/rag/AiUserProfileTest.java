package com.bodeum.domain.ai.model.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiUserProfileTest {

    @Test
    void normalizesNullCollectionsToEmptyLists() {
        AiUserProfile profile = new AiUserProfile(null, null, null, null, null);

        assertThat(profile.disabilityTypes()).isEmpty();
        assertThat(profile.interests()).isEmpty();
    }
}
