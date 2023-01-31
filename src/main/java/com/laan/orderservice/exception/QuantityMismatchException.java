package com.laan.orderservice.exception;

public class QuantityMismatchException extends RuntimeException {

    public QuantityMismatchException(String message) {
        super(message);
    }
}
