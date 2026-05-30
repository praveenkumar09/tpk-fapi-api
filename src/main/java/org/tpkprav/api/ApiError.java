package org.tpkprav.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<ApiErrorDetail> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
