package com.aliyar.currency_service.modules.exception;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super("Start date must be before end date");
    }
}