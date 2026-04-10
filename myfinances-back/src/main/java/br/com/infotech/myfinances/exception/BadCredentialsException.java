package br.com.infotech.myfinances.exception;

public class BadCredentialsException extends BusinessException {
  public BadCredentialsException(String message) {
    super(message);
  }
}
