package com.bodeum.domain.info.dto.response;

public record InfoItemShareResponse(
        Long infoItemId,
        String shareUrl
) {
    public static InfoItemShareResponse of(Long infoItemId, String shareUrl) {
        return new InfoItemShareResponse(infoItemId, shareUrl);
    }
}