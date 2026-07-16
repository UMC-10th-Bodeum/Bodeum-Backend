package com.bodeum.global.infrastructure.storage;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements BaseErrorCode {

    EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "STORAGE400_1", "업로드할 이미지 파일이 비어 있습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "STORAGE400_2", "지원하지 않는 이미지 형식입니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE500_1", "이미지 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
