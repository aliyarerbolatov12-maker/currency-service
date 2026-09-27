package com.aliyar.currency_service.modules.exception;

public class HistoryPeriodTooLargeException extends RuntimeException {

    public HistoryPeriodTooLargeException(long maxDays) {
        super("Maximum history period is " + maxDays + " days");
    }
}