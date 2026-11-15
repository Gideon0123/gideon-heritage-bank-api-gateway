
package com.gideonheritagebank.API_GATEWAY.utils;

public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLE = "X-User-Role";
    public static final String CORRELATION_ID = "X-Correlation-ID";

    private GatewayHeaders() {
        throw new UnsupportedOperationException("Utility class");
    }
}

