package com.bodeum.domain.ai.model.answer;

import java.util.List;

public record GeneratedAiAnswer(String answer, List<String> citedDocumentKeys) {
}
