package com.bodeum.domain.info.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;

public class InfoException extends ProjectException {

    public InfoException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}