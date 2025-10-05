package com.example.moviesapi.Entity;

import lombok.Getter;

import java.util.List;

@Getter
public class Error {
    private final String code;
    private final String message;
    private List<?> details;

    public Error(String code, String message) {
        this.code = code;
        this.message = message;
    }
    public Error(String code, String message, List<?> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
