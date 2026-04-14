package br.com.infotech.myfinances.exception;

public class LoginAlreadyExistsException extends BusinessException {
  public LoginAlreadyExistsException(String message) {
    super(message);
  }
}
