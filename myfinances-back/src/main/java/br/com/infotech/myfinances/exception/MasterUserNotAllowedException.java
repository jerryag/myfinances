package br.com.infotech.myfinances.exception;

public class MasterUserNotAllowedException extends BusinessException {
  public MasterUserNotAllowedException(String message) {
    super(message);
  }
}
