package com.bodeum.domain.ai.infrastructure.support;

import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 최종 AI 답변에 저장·노출할 출처를 출처 식별자와 정규화된 전체 URL 기준으로 정리한다.
 */
public final class AiSourceDeduplicator {

    private AiSourceDeduplicator() {
    }

    public static List<AiReferenceDocument> deduplicate(
            List<AiReferenceDocument> sources
    ) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        Set<String> seenSourceKeys = new HashSet<>();
        Set<String> seenUrls = new HashSet<>();
        List<AiReferenceDocument> distinctSources = new ArrayList<>();
        for (AiReferenceDocument source : sources) {
            if (source == null) {
                continue;
            }
            String sourceKey = source.sourceType() == null || source.sourceId() == null
                    ? "" : source.sourceType() + ":" + source.sourceId();
            String normalizedUrl = normalizeUrl(source.url());
            if (!sourceKey.isBlank() && !seenSourceKeys.add(sourceKey)) {
                continue;
            }
            if (!normalizedUrl.isBlank() && !seenUrls.add(normalizedUrl)) {
                continue;
            }
            distinctSources.add(source);
        }
        return List.copyOf(distinctSources);
    }

    static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url.contains("://") ? url.trim() : "https://" + url.trim())
                    .normalize();
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return fallbackUrl(url);
            }
            String scheme = uri.getScheme() == null
                    ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String normalizedHost = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
            int port = uri.getPort();
            String portText = port < 0 || (port == 80 && "http".equals(scheme))
                    || (port == 443 && "https".equals(scheme)) ? "" : ":" + port;
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            return scheme + "://" + normalizedHost + portText + path + query;
        } catch (IllegalArgumentException ignored) {
            return fallbackUrl(url);
        }
    }

    private static String fallbackUrl(String url) {
        return url.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("#.*$", "")
                .replaceAll("/+$", "");
    }
}
