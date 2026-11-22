package com.hotelbooking.api.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<String> details
) {
}
