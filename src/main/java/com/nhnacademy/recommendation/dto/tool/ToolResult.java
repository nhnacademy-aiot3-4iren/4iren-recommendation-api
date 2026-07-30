package com.nhnacademy.recommendation.dto.tool;

public record ToolResult<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    public static <T> ToolResult<T> success(T data) {
        return new ToolResult<>(true, "SUCCESS", null, data);
    }

    public static <T> ToolResult<T> failure(String code, String message) {
        return new ToolResult<>(false, code, message, null);
    }
}
