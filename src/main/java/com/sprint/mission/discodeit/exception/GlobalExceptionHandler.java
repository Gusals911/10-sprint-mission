package com.sprint.mission.discodeit.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DiscodeitException.class)
    public ResponseEntity<ErrorResponse> customExceptionHandler(DiscodeitException e){
        ErrorResponse response = new ErrorResponse(
                e.getTimestamp(),
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage(),
                e.getDetails(),
                e.getClass().getSimpleName(),
                e.getErrorCode().getStatus().value()
                );
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                errorCode.getCode(),
                errorCode.getMessage(),
                Map.of("reason", e.getMessage()),
                e.getClass().getSimpleName(),
                errorCode.getStatus().value()
        );
        return ResponseEntity.internalServerError().body(response);
    }
}