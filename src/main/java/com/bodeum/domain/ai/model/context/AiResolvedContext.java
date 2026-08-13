package com.bodeum.domain.ai.model.context;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

public record AiResolvedContext(
        String topic,
        RegionContext region,
        Map<String, String> filters,
        String requestedInformation,
        Integer requestedResultCount
) {

    public AiResolvedContext {
        topic = normalize(topic);
        filters = normalizeFilters(filters);
        requestedInformation = normalize(requestedInformation);
        requestedResultCount = requestedResultCount != null && requestedResultCount > 0
                ? requestedResultCount
                : null;
    }

    public boolean isEmpty() {
        return topic == null
                && region == null
                && filters.isEmpty()
                && requestedInformation == null
                && requestedResultCount == null;
    }

    public AiResolvedContext merge(AiResolvedContext update) {
        if (update == null || update.isEmpty()) {
            return this;
        }
        Map<String, String> mergedFilters = new LinkedHashMap<>(filters);
        mergedFilters.putAll(update.filters);
        return new AiResolvedContext(
                update.topic == null ? topic : update.topic,
                update.region == null ? region : update.region,
                mergedFilters,
                update.requestedInformation == null
                        ? requestedInformation
                        : update.requestedInformation,
                update.requestedResultCount == null
                        ? requestedResultCount
                        : update.requestedResultCount
        );
    }

    public AiResolvedContext withRegion(
            String regionLevel1,
            String regionLevel2
    ) {
        return new AiResolvedContext(
                topic,
                new RegionContext(regionLevel1, regionLevel2),
                filters,
                requestedInformation,
                requestedResultCount
        );
    }

    public AiResolvedContext withFilter(String key, String value) {
        Map<String, String> updatedFilters = new LinkedHashMap<>(filters);
        String normalizedKey = normalize(key);
        String normalizedValue = normalize(value);
        if (normalizedKey != null && normalizedValue != null) {
            updatedFilters.put(normalizedKey, normalizedValue);
        }
        return new AiResolvedContext(
                topic, region, updatedFilters, requestedInformation, requestedResultCount);
    }

    public AiResolvedContext withRequestedInformation(String information) {
        return new AiResolvedContext(topic, region, filters, information, requestedResultCount);
    }

    public AiResolvedContext withRequestedResultCount(Integer count) {
        return new AiResolvedContext(topic, region, filters, requestedInformation, count);
    }

    public String toResolvedQuestion(String fallback) {
        if (topic == null) {
            return fallback;
        }
        StringBuilder question = new StringBuilder();
        if (region != null && !region.displayName().isBlank()) {
            question.append(region.displayName()).append(' ');
        }
        filters.values().stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .forEach(value -> question.append(value).append(' '));
        question.append(topic);
        if (requestedInformation != null
                && !"목록".equals(requestedInformation)) {
            question.append(' ').append(requestedInformation);
        }
        question.append(" 알려줘");
        return question.toString().trim();
    }

    public String toPromptText() {
        return "topic=" + value(topic)
                + ", region=" + (region == null ? "null" : region.displayName())
                + ", filters=" + filters
                + ", requestedInformation=" + value(requestedInformation)
                + ", requestedResultCount=" + requestedResultCount;
    }

    private static Map<String, String> normalizeFilters(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        filters.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            String normalizedValue = normalize(value);
            if (normalizedKey != null && normalizedValue != null) {
                normalized.put(normalizedKey, normalizedValue);
            }
        });
        return Collections.unmodifiableMap(normalized);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String value(String value) {
        return value == null ? "null" : value;
    }

    public record RegionContext(String sido, String sigungu) {

        public RegionContext {
            sido = normalize(sido);
            sigungu = normalize(sigungu);
        }

        public String displayName() {
            if (sido == null) {
                return sigungu == null ? "" : sigungu;
            }
            return sigungu == null ? sido : sido + " " + sigungu;
        }
    }
}
