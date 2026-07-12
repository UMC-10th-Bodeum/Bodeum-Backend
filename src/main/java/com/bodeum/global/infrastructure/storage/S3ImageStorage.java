package com.bodeum.global.infrastructure.storage;

import com.bodeum.global.apiPayload.exception.ProjectException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3ImageStorage {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    /**
     * 이미지 파일을 지정한 디렉터리 아래에 업로드하고 공개 접근 URL을 반환한다.
     * 파일명은 충돌을 피하기 위해 UUID로 생성한다.
     */
    public String upload(MultipartFile file, String directory) {
        String extension = validateAndResolveExtension(file);
        String key = directory + "/" + UUID.randomUUID() + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | SdkException e) {
            throw new ProjectException(StorageErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return resolvePublicUrl(key);
    }

    private String validateAndResolveExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProjectException(StorageErrorCode.EMPTY_IMAGE_FILE);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new ProjectException(StorageErrorCode.INVALID_IMAGE_TYPE);
        }

        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new ProjectException(StorageErrorCode.INVALID_IMAGE_TYPE);
        }

        return extension;
    }

    private String resolvePublicUrl(String key) {
        if (StringUtils.hasText(s3Properties.getPublicBaseUrl())) {
            String baseUrl = s3Properties.getPublicBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + "/" + key;
        }

        return "https://" + s3Properties.getBucket()
                + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }
}
