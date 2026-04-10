package br.com.infotech.myfinances.exception;

public class ValidationException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
