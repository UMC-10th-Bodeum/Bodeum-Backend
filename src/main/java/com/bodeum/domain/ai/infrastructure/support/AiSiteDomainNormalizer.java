package com.bodeum.domain.ai.infrastructure.support;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

public final class AiSiteDomainNormalizer {

    private AiSiteDomainNormalizer() {
    }

    public static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String absoluteUrl = url.contains("://") ? url : "https://" + url;
            String host = URI.create(absoluteUrl.trim()).normalize().getHost();
            return host == null ? null : registrableHost(host);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String registrableHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        String[] labels = normalized.split("\\.");
        if (labels.length <= 2) {
            return normalized;
        }
        int labelCount = "kr".equals(labels[labels.length - 1]) ? 3 : 2;
        return String.join(
                ".",
                Arrays.copyOfRange(labels, labels.length - labelCount, labels.length)
        );
    }
}
