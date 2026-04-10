package br.com.infotech.myfinances.exception;

public class BlockedUserException extends BusinessException {
  public BlockedUserException(String message) {
    super(message);
  }
}
