package br.com.infotech.myfinances.exception;

public class InvalidNewPasswordDataException extends RuntimeException {
    public InvalidNewPasswordDataException(String message) {
        super(message);
    }
}
