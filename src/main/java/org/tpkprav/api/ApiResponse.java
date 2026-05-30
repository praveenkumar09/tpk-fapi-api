package org.tpkprav.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String timestamp,
        String requestId,
        Status status,
        T data,
        ApiError error
) {

    public static <T> ApiResponse<T> success(String requestId, T data) {
        return new ApiResponse<>(now(), requestId, Status.SUCCESS, data, null);
    }

    public static ApiResponse<Object> error(String requestId, ApiError error) {
        return new ApiResponse<>(now(), requestId, Status.ERROR, null, error);
    }

    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
