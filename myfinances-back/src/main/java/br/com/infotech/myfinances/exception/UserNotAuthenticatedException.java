package br.com.infotech.myfinances.exception;

public class UserNotAuthenticatedException extends BusinessException {
  public UserNotAuthenticatedException(String message) {
    super(message);
  }
}
