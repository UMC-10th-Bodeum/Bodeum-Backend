package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import java.util.Optional;

public interface AiStarterQuestionClassifier {

    Optional<AiStarterQuestionType> classify(String question);
}
