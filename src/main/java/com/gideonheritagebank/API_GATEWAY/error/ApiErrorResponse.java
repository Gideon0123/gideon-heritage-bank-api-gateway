package com.gideonheritagebank.API_GATEWAY.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        boolean success,
        int status,
        String message,
        Map<String, String> errors,
        String path,
        String traceId
) {
}
