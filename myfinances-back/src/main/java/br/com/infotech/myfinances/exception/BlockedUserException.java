package br.com.infotech.myfinances.exception;

public class BlockedUserException extends RuntimeException {
    public BlockedUserException(String message) {
        super(message);
    }
}
