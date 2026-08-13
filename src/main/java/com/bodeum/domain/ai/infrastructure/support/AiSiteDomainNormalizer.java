package com.bodeum.domain.ai.infrastructure.support;

import com.google.common.net.InternetDomainName;
import java.net.URI;
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
        try {
            InternetDomainName domainName = InternetDomainName.from(normalized);
            if (!domainName.hasPublicSuffix()) {
                return normalized;
            }
            if (!domainName.isUnderPublicSuffix()) {
                return null;
            }
            return domainName.topPrivateDomain().toString();
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return normalized;
        }
    }
}
