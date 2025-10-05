package com.example.moviesapi.Entity;


public enum ErrorCode  implements java.io.Serializable {
    BAD_REQUEST("BAD_REQUEST", "Invalid parameters"),
    UNAUTHORIZED("UNAUTHORIZED", "Missing or invalid authentication information"),
    FORBIDDEN("FORBIDDEN", "No permission to perform this operation"),
    NOT_FOUND("NOT_FOUND", "Resource not found");

    ErrorCode(String notFound, String s) {
    }
}
