package com.bodeum.global.infrastructure.storage;

import com.bodeum.global.apiPayload.exception.ProjectException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorage {

    static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

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
            log.error("S3 이미지 업로드 실패 key={}", key, e);
            throw new ProjectException(StorageErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return resolvePublicUrl(key);
    }

    /**
     * 공개 URL로 저장된 이미지의 S3 실제 객체를 삭제한다. 회원 탈퇴 시 프로필 이미지(개인정보)를 파기하는 데 사용한다.
     * URL이 비어 있거나 이 스토리지가 관리하는 형식(공개 base URL 또는 표준 S3 URL)이 아니면 조용히 무시한다.
     */
    public void delete(String imageUrl) {
        String key = extractKey(imageUrl);
        if (key == null) {
            // URL이 이 스토리지가 관리하는 형식과 다르면 S3 삭제를 건너뛴다. publicBaseUrl 설정 변경 등으로
            // 과거 URL이 인식되지 않아 삭제가 누락될 수 있으므로, 운영 중 감지할 수 있게 로그를 남긴다.
            if (StringUtils.hasText(imageUrl)) {
                log.warn("S3 삭제 스킵: 인식할 수 없는 이미지 URL 형식 - {}", imageUrl);
            }
            return;
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(s3Properties.getBucket())
                        .key(key)
                        .build()
        );
    }

    // 업로드 시 resolvePublicUrl이 만든 공개 URL에서 S3 object key를 역산한다.
    private String extractKey(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        String prefix = publicUrlPrefix();
        if (imageUrl.startsWith(prefix)) {
            return stripLeadingSlash(imageUrl.substring(prefix.length()));
        }

        String standardPrefix = standardS3UrlPrefix();
        if (imageUrl.startsWith(standardPrefix)) {
            return stripLeadingSlash(imageUrl.substring(standardPrefix.length()));
        }

        return null;
    }

    private String publicUrlPrefix() {
        if (StringUtils.hasText(s3Properties.getPublicBaseUrl())) {
            String baseUrl = s3Properties.getPublicBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + "/";
        }
        return standardS3UrlPrefix();
    }

    private String standardS3UrlPrefix() {
        return "https://" + s3Properties.getBucket()
                + ".s3." + s3Properties.getRegion() + ".amazonaws.com/";
    }

    private String stripLeadingSlash(String value) {
        String key = value.startsWith("/") ? value.substring(1) : value;
        return key.isBlank() ? null : key;
    }

    private String validateAndResolveExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProjectException(StorageErrorCode.EMPTY_IMAGE_FILE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ProjectException(StorageErrorCode.IMAGE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new ProjectException(StorageErrorCode.INVALID_IMAGE_TYPE);
        }

        // "image/jpeg; charset=utf-8"처럼 파라미터가 붙어 와도 미디어 타입만으로 판별한다.
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_CONTENT_TYPES.get(mediaType);
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
