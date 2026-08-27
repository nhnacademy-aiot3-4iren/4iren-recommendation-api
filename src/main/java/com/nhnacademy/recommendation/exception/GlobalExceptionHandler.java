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

    @ExceptionHandler(ProbabilityRangeException.class)
    public ResponseEntity<ErrorResponse> badRequest(ProbabilityRangeException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(InvalidPolicyRangeException.class)
    public ResponseEntity<ErrorResponse> badRequest(InvalidPolicyRangeException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(PolicyNotFoundException e){

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(RoomPreferenceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(RoomPreferenceNotFoundException e){

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(PolicyAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(PolicyAccessDeniedException e){

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(PolicyDuplicateException.class)
    public ResponseEntity<ErrorResponse> conflict(PolicyDuplicateException e){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                e.getMessage(),
                LocalDateTime.now()
        ));
    }
}
