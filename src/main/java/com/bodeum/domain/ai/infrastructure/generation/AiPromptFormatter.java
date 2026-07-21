package com.bodeum.domain.ai.infrastructure.generation;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class AiPromptFormatter {

    public String formatProfile(AiUserProfile profile) {
        return """
                [사용자 맞춤 정보]
                활동 지역: %s
                관심사: %s

                [자녀 관련 정보]
                자녀 나이: %s
                집중 케어 영역: %s
                자녀 관련 키워드: %s
                """.formatted(
                valueOrNotProvided(profile.region()),
                valueOrNotProvided(profile.interests()),
                valueOrNotProvided(profile.childAge()),
                valueOrNotProvided(profile.disabilityTypes()),
                valueOrNotProvided(profile.keywordText())
        );
    }

    private String valueOrNotProvided(Object value) {
        if (value == null) {
            return "입력되지 않음";
        }
        if (value instanceof String text && text.isBlank()) {
            return "입력되지 않음";
        }
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            return "입력되지 않음";
        }
        return value.toString();
    }
}
