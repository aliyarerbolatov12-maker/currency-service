package com.aliyar.currency_service.common.exception;

import com.aliyar.currency_service.modules.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProviderNotFoundException.class)
    public ProblemDetail handleProviderNotFound(
            ProviderNotFoundException ex,
            HttpServletRequest request) {

        log.warn(ex.getMessage());

        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Provider not found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(RateNotFoundException.class)
    public ProblemDetail handleRateNotFound(
            RateNotFoundException ex,
            HttpServletRequest request) {

        log.warn(ex.getMessage());

        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Exchange rate not found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(HistoryPeriodTooLargeException.class)
    public ProblemDetail handleHistoryPeriodTooLarge(
            HistoryPeriodTooLargeException ex,
            HttpServletRequest request) {

        log.warn(ex.getMessage());

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "History period too large",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(
            InvalidDateRangeException ex,
            HttpServletRequest request) {

        log.warn(ex.getMessage());

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid date range",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCurrencyPairException.class)
    public ProblemDetail handleInvalidCurrencyPair(
            InvalidCurrencyPairException ex,
            HttpServletRequest request) {

        log.warn(ex.getMessage());

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid currency pair",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                request
        );

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error", ex);

        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Unexpected server error",
                request
        );
    }

    private ProblemDetail buildProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle(title);
        problem.setDetail(detail);

        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}