package com.example.moviesapi.BO;



import java.util.List;
import java.util.Map;

/**
 * API response returned by API call.
 */
public record ApiResponse<T>(int statusCode, Map<String, List<String>> headers, T data) {
    public ApiResponse(int statusCode, Map<String, List<String>> headers) {
        this(statusCode, headers, null);
    }
    public ApiResponse {
    }

}
