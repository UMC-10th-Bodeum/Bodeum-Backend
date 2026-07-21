package com.bodeum.domain.ai.infrastructure.support;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

public final class AiTimeoutDetector {

    private AiTimeoutDetector() {
    }

    public static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
