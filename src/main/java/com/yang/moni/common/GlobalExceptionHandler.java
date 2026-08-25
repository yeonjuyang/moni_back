package com.yang.moni.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleNoSuchElement(NoSuchElementException ex) {
        return Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Not found");
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleIllegalState(IllegalStateException ex) {
        return Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Forbidden");
    }

}
