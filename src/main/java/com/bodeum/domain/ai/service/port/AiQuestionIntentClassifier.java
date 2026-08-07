package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.enums.AiQuestionIntent;

public interface AiQuestionIntentClassifier {

    AiQuestionIntent classify(String question);
}
