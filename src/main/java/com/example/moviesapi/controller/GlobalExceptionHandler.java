package com.example.moviesapi.controller;

import com.example.moviesapi.Entity.Error;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Error error = new Error("UNPROCESSABLE_ENTITY", "Invalid JSON format");
        return ResponseEntity.status(422).body(error);
    }
}