package com.bodeum.global.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bodeum.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageTest {

    @Mock
    private S3Client s3Client;

    private S3Properties s3Properties;
    private S3ImageStorage s3ImageStorage;

    @BeforeEach
    void setUp() {
        s3Properties = new S3Properties();
        s3Properties.setBucket("bodeum-bucket");
        s3Properties.setRegion("ap-northeast-2");
        s3ImageStorage = new S3ImageStorage(s3Client, s3Properties);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> s3ImageStorage.upload(empty, "profile-images"))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(StorageErrorCode.EMPTY_IMAGE_FILE);
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile pdf = new MockMultipartFile("image", "a.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> s3ImageStorage.upload(pdf, "profile-images"))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(StorageErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    void rejectsMissingContentType() {
        MockMultipartFile image = new MockMultipartFile("image", "a.png", null, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> s3ImageStorage.upload(image, "profile-images"))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(StorageErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    void uploadsAndReturnsStandardS3Url() {
        MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

        String url = s3ImageStorage.upload(image, "profile-images");

        assertThat(url).startsWith("https://bodeum-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/");
        assertThat(url).endsWith(".png");
        assertPutObjectRequest("profile-images/", ".png", "image/png");
    }

    @Test
    void usesPublicBaseUrlWhenConfigured() {
        s3Properties.setPublicBaseUrl("https://cdn.bodeum.com/");
        MockMultipartFile image = new MockMultipartFile("image", "a.webp", "image/webp", new byte[]{1, 2, 3});

        String url = s3ImageStorage.upload(image, "profile-images");

        // 뒤 슬래시가 중복되지 않고 CDN 도메인 기준으로 붙는다.
        assertThat(url).startsWith("https://cdn.bodeum.com/profile-images/");
        assertThat(url).endsWith(".webp");
        assertPutObjectRequest("profile-images/", ".webp", "image/webp");
    }

    @Test
    void wrapsS3FailureAsUploadFailed() {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(SdkClientException.create("boom"));

        assertThatThrownBy(() -> s3ImageStorage.upload(image, "profile-images"))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(StorageErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    void deleteRemovesObjectByKeyFromStandardS3Url() {
        String url = "https://bodeum-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/abc.png";

        s3ImageStorage.delete(url);

        assertDeleteObjectKey("profile-images/abc.png");
    }

    @Test
    void deleteRemovesObjectByKeyFromPublicBaseUrl() {
        s3Properties.setPublicBaseUrl("https://cdn.bodeum.com/");
        String url = "https://cdn.bodeum.com/profile-images/abc.webp";

        s3ImageStorage.delete(url);

        assertDeleteObjectKey("profile-images/abc.webp");
    }

    @Test
    void deleteSkipsUnrecognizedUrl() {
        // publicBaseUrl도 표준 S3 URL도 아닌 형식은 key를 역산할 수 없어 삭제를 건너뛴다.
        s3ImageStorage.delete("https://other.example.com/profile-images/abc.png");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteSkipsBlankUrl() {
        s3ImageStorage.delete(null);
        s3ImageStorage.delete("   ");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    private void assertDeleteObjectKey(String expectedKey) {
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("bodeum-bucket");
        assertThat(request.key()).isEqualTo(expectedKey);
    }

    private void assertPutObjectRequest(String keyPrefix, String keySuffix, String contentType) {
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("bodeum-bucket");
        assertThat(request.key()).startsWith(keyPrefix);
        assertThat(request.key()).endsWith(keySuffix);
        assertThat(request.contentType()).isEqualTo(contentType);
    }
}
