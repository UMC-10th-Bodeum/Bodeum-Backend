package com.bodeum.domain.ai.infrastructure;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

final class AiTimeoutDetector {

    private AiTimeoutDetector() {
    }

    static boolean isTimeout(Throwable throwable) {
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
