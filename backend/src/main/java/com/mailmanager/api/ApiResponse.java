package com.mailmanager.api;

public record ApiResponse<T>(
        String traceId,
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(String traceId, T data) {
        return new ApiResponse<>(traceId, "OK", "success", data);
    }

    public static <T> ApiResponse<T> success(String traceId, String message, T data) {
        return new ApiResponse<>(traceId, "OK", message, data);
    }

    public static <T> ApiResponse<T> failure(String traceId, String code, String message) {
        return new ApiResponse<>(traceId, code, message, null);
    }
}
