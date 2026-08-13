package com.bodeum.domain.ai.model.context;

import java.util.List;

public record AiSearchQueryContext(String question, List<String> queries) {
}
