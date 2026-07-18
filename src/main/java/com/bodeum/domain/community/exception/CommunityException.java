package com.bodeum.domain.community.exception;

import com.bodeum.global.apiPayload.exception.ProjectException;

public class CommunityException extends ProjectException {

    public CommunityException(CommunityErrorCode errorCode) {
        super(errorCode);
    }
}
