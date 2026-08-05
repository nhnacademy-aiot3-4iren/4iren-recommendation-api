package com.nhnacademy.recommendation.exception;

import com.nhnacademy.recommendation.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidMessageException.class)
    public ResponseEntity<ErrorResponse> badRequest(InvalidMessageException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(NotPositiveValueException.class)
    public ResponseEntity<ErrorResponse> badRequest(NotPositiveValueException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(RequiredValueException.class)
    public ResponseEntity<ErrorResponse> badRequest(RequiredValueException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }
}
