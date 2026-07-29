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
            URI uri = URI.create(value).normalize();
            if (uri.getScheme() == null) {
                uri = URI.create("https://" + value).normalize();
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("URL host is required");
            }

            StringBuilder normalized = new StringBuilder()
                    .append(uri.getScheme().toLowerCase(Locale.ROOT))
                    .append("://");
            if (uri.getRawUserInfo() != null) {
                normalized.append(uri.getRawUserInfo()).append('@');
            }
            if (host.contains(":")) {
                normalized.append('[').append(host.toLowerCase(Locale.ROOT)).append(']');
            } else {
                normalized.append(host.toLowerCase(Locale.ROOT));
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
