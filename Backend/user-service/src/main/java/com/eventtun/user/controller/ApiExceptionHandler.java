package com.eventtun.user.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Standard Spring HTTP errors use the Chapter 2 Problem Details format. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException exception) {
        // Handles duplicate emails even when requests arrive simultaneously.
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The data conflicts with an existing record.");
    }
}
