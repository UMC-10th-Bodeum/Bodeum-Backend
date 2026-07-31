package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.net.URI;
import java.util.Locale;

public final class AiUrlNormalizer {

    private AiUrlNormalizer() {
    }

    public static String normalize(String url) {
        try {
            String value = url.trim();
            String absoluteUrl = value.startsWith("//")
                    ? "https:" + value
                    : value.contains("://") ? value : "https://" + value;
            URI uri = URI.create(absoluteUrl).normalize();
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("URL scheme must be http or https");
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("URL host is required");
            }
            if (uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("URL userinfo is not allowed");
            }

            StringBuilder normalized = new StringBuilder()
                    .append(scheme)
                    .append("://");
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
                normalized.append(normalizedHost);
            } else if (normalizedHost.contains(":")) {
                normalized.append('[').append(normalizedHost).append(']');
            } else {
                normalized.append(normalizedHost);
            }
            if (uri.getPort() >= 0) {
                normalized.append(':').append(uri.getPort());
            }
            if (uri.getRawPath() != null) {
                normalized.append(uri.getRawPath());
            }
            if (uri.getRawQuery() != null) {
                normalized.append('?').append(uri.getRawQuery());
            }
            return normalized.toString();
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }
}
