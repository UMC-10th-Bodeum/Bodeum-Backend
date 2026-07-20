package com.bodeum.domain.search.exception;

import com.bodeum.global.apiPayload.exception.ProjectException;

public class SearchException extends ProjectException {

    public SearchException(SearchErrorCode errorCode) {
        super(errorCode);
    }
}
