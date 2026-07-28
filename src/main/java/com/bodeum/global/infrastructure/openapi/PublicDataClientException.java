package com.bodeum.global.infrastructure.openapi;

public class PublicDataClientException extends RuntimeException {

    public PublicDataClientException(String message) {
        super(message);
    }

    public PublicDataClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
