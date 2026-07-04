package com.bodeum.global.apiPayload.handler;

import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.BaseErrorCode;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneralExceptionAdvice {

    @ExceptionHandler(ProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberException(
            ProjectException e
    ) {
        BaseErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST;
        String message = Stream.concat(
                        e.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage()),
                        e.getBindingResult()
                                .getGlobalErrors()
                                .stream()
                                .map(error -> error.getDefaultMessage())
                )
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode, message));
    }

    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(
            Exception ex
    ) {
        BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                        code,
                        ex.getMessage()
                ));
    }
}
