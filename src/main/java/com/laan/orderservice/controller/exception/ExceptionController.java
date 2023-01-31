package com.laan.orderservice.controller.exception;

import com.laan.orderservice.exception.*;
import com.laan.orderservice.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class ExceptionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(value = {UserNotFoundException.class, ProductNotFoundException.class, OrderNotFoundException.class})
    public ResponseEntity<Object> handleNotFoundExceptions(RuntimeException exception) {
        LOGGER.error("Not found exception occurred. {}", exception.getMessage());
        return new ResponseEntity<>(createErrorResponse(exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {QuantityMismatchException.class, InvalidOrderStatusException.class})
    public ResponseEntity<Object> handleOrderServiceExceptions(RuntimeException exception) {
        LOGGER.error("Order service exception occurred. {}", exception.getMessage());
        return new ResponseEntity<>(createErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        StringBuilder stringBuilder = new StringBuilder();
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        for (FieldError fieldError : fieldErrors) {
            stringBuilder.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage()).append(" ");
        }
        LOGGER.error("MethodArgumentNotValidException occurred. {}", stringBuilder);
        return new ResponseEntity<>(createErrorResponse(stringBuilder.toString()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<Object> handleException(Exception exception) {
        LOGGER.error("Exception occurred.", exception);
        return new ResponseEntity<>(createErrorResponse(exception.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
    }

    private ErrorResponse createErrorResponse(String message) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(message);
        return errorResponse;
    }

}
