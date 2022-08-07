package com.matzip.server.global.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MatzipControllerAdvice {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @ExceptionHandler(value = InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(MatzipException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.errorType.getErrorCode(), e.errorType.name(), e.detail),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(value = NotAllowedException.class)
    public ResponseEntity<ErrorResponse> notAllowed(MatzipException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.errorType.getErrorCode(), e.errorType.name(), e.detail),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(value = DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(MatzipException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.errorType.getErrorCode(), e.errorType.name(), e.detail),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(value = ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(MatzipException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.errorType.getErrorCode(), e.errorType.name(), e.detail),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(value = ServerErrorException.class)
    public ResponseEntity<ErrorResponse> serverError(MatzipException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.errorType.getErrorCode(), e.errorType.name(), e.detail),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}