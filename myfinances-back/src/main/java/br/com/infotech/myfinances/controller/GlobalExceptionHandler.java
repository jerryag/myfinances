package br.com.infotech.myfinances.controller;

import br.com.infotech.myfinances.exception.BadCredentialsException;
import br.com.infotech.myfinances.exception.BlockedUserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(BlockedUserException.class)
    public ResponseEntity<String> handleBlockedUser(BlockedUserException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(br.com.infotech.myfinances.exception.InvalidNewPasswordDataException.class)
    public ResponseEntity<String> handleInvalidNewPasswordDataException(
            br.com.infotech.myfinances.exception.InvalidNewPasswordDataException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
